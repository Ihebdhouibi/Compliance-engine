package com.devteam.aiauditserver.services.project.audit;

import com.devteam.aiauditserver.enums.Project.Pathway;
import com.devteam.aiauditserver.models.project.AuditRequest.AuditRequest;
import com.devteam.aiauditserver.models.project.AuditRequest.AuditRequestAnswer;
import com.devteam.aiauditserver.models.project.AuditRequest.RoutingProfile;
import com.devteam.aiauditserver.repositories.project.RoutingProfileRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Deterministic Level-1 → Level-2 routing engine. Loads the shared rules
 * from {@code routing/level1_routing_rules.v1.json} on the classpath
 * (same file Python uses for explanations) and evaluates them against the
 * answer map.
 */
@Service
public class RoutingEngine {

    private static final Logger logger = LoggerFactory.getLogger(RoutingEngine.class);
    private static final String RULES_PATH = "routing/level1_routing_rules.v1.json";

    private final ObjectMapper mapper = new ObjectMapper();
    private final RoutingProfileRepository profileRepo;
    private JsonNode rules;

    public RoutingEngine(RoutingProfileRepository profileRepo) {
        this.profileRepo = profileRepo;
    }

    @PostConstruct
    public void load() {
        try (InputStream in = new ClassPathResource(RULES_PATH).getInputStream()) {
            rules = mapper.readTree(in);
            logger.info("RoutingEngine loaded rules v{} ({} pathways)",
                    rules.path("version").asText(), rules.path("pathways").size());
        } catch (IOException e) {
            logger.error("Failed to load routing rules from {}", RULES_PATH, e);
            rules = mapper.createObjectNode();
        }
    }

    /** Build a routing profile for a request using the answers attached to it. */
    public RoutingProfile computeFor(AuditRequest request) {
        Map<String, String> answers = answerMap(request);
        return computeFromAnswers(request, answers);
    }

    public RoutingProfile computeFromAnswers(AuditRequest request, Map<String, String> answers) {
        RoutingProfile profile = profileRepo.findByAuditRequestId(request.getId())
                .orElseGet(RoutingProfile::new);
        profile.setAuditRequest(request);

        profile.setFirmSizeCategory(bucketFor("firmSizeCategory", answers.get("selector_firm_size")));
        profile.setAiAdoptionCategory(bucketFor("aiAdoptionCategory", answers.get("selector_ai_use")));
        profile.setSectorCategory(answers.get("selector_org_type"));
        profile.setAiImpactCategory("Yes".equalsIgnoreCase(answers.get("selector_client_facing_ai"))
                ? "CLIENT_FACING" : "INTERNAL");
        profile.setRegulatoryExposureCategory(
                regulatedSectorsSet().contains(answers.get("selector_org_type"))
                        ? "REGULATED" : "UNREGULATED");
        profile.setDataSensitivityCategory(profile.getRegulatoryExposureCategory());

        JsonNode matched = firstMatchingPathway(answers);
        if (matched == null) {
            // safety net — should not happen because PATHWAY_1 has empty match
            matched = mapper.createObjectNode();
        }

        Pathway pathway;
        try {
            pathway = Pathway.valueOf(matched.path("id").asText("PATHWAY_1"));
        } catch (IllegalArgumentException ex) {
            pathway = Pathway.PATHWAY_1;
        }
        profile.setPathway(pathway);
        profile.setRecommendedPackage(matched.path("recommendedPackage").asText("RICS_AWARENESS_BASELINE"));
        profile.setEvidenceDepth(matched.path("evidenceDepth").asText("LOW"));

        List<String> modules = new ArrayList<>();
        matched.path("activeModules").forEach(n -> modules.add(n.asText()));
        profile.setActiveModules(String.join(",", modules));

        // Price band with size adjustment
        JsonNode price = matched.path("indicativePrice");
        int min = price.path("min").asInt(0);
        int max = price.path("max").asInt(0);
        JsonNode adj = rules.path("sizeAdjustments").path(
                profile.getFirmSizeCategory() == null ? "MEDIUM" : profile.getFirmSizeCategory());
        double adjMin = adj.path("min").asDouble(1.0);
        double adjMax = adj.path("max").asDouble(1.0);
        profile.setIndicativePriceMin((int) Math.round(min * adjMin));
        profile.setIndicativePriceMax((int) Math.round(max * adjMax));
        profile.setPriceCurrency(price.path("currency").asText("GBP"));
        profile.setPriceStatus(price.path("status").asText("INDICATIVE"));

        profile.setRationale(rules.path("explanations").path(pathway.name())
                .asText("Recommended based on your Level 1 responses."));
        profile.setComputedAt(new Date());

        return profileRepo.save(profile);
    }

    // ── helpers ─────────────────────────────────────────────────────────

    private Map<String, String> answerMap(AuditRequest req) {
        Map<String, String> map = new HashMap<>();
        if (req.getAnswers() == null) return map;
        for (AuditRequestAnswer a : req.getAnswers()) {
            if (a.getFieldKey() != null && a.getAnswerValue() != null) {
                map.put(a.getFieldKey(), a.getAnswerValue());
            }
        }
        return map;
    }

    private Set<String> regulatedSectorsSet() {
        JsonNode arr = rules.path("buckets").path("regulatedSectors");
        return StreamSupport.stream(arr.spliterator(), false)
                .map(JsonNode::asText).collect(Collectors.toSet());
    }

    private String bucketFor(String bucketName, String value) {
        if (value == null) return null;
        JsonNode buckets = rules.path("buckets").path(bucketName);
        Iterator<Map.Entry<String, JsonNode>> it = buckets.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> e = it.next();
            for (JsonNode v : e.getValue()) {
                if (v.asText().equalsIgnoreCase(value)) return e.getKey();
            }
        }
        return null;
    }

    private JsonNode firstMatchingPathway(Map<String, String> answers) {
        for (JsonNode p : rules.path("pathways")) {
            if (matches(p.path("match"), answers)) return p;
        }
        return null;
    }

    private boolean matches(JsonNode match, Map<String, String> answers) {
        if (match == null || match.isMissingNode() || match.isNull()) return true;
        if (match.has("all")) {
            for (JsonNode cond : match.path("all")) {
                if (!evalCondition(cond, answers)) return false;
            }
            return true;
        }
        if (match.has("any")) {
            for (JsonNode cond : match.path("any")) {
                if (evalCondition(cond, answers)) return true;
            }
            return false;
        }
        return evalCondition(match, answers);
    }

    private boolean evalCondition(JsonNode cond, Map<String, String> answers) {
        String key = cond.path("fieldKey").asText();
        String op = cond.path("op").asText("equals");
        String value = answers.get(key);
        switch (op) {
            case "equals":
                return cond.path("value").asText("").equalsIgnoreCase(value == null ? "" : value);
            case "in":
                for (JsonNode v : cond.path("values")) {
                    if (v.asText().equalsIgnoreCase(value)) return true;
                }
                return false;
            case "present":
                return value != null && !value.isEmpty();
            default:
                return false;
        }
    }
}

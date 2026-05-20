package com.devteam.aiauditserver.services.project.audit;

import com.devteam.aiauditserver.enums.Project.AuditLevel;
import com.devteam.aiauditserver.enums.Project.AuditType;
import com.devteam.aiauditserver.enums.Project.FieldType;
import com.devteam.aiauditserver.models.project.AuditForm.AuditFormField;
import com.devteam.aiauditserver.models.project.AuditForm.AuditFormFieldOption;
import com.devteam.aiauditserver.models.project.AuditForm.AuditFormOptionList;
import com.devteam.aiauditserver.models.project.AuditForm.AuditFormStep;
import com.devteam.aiauditserver.models.project.AuditForm.AuditFormTemplate;
import com.devteam.aiauditserver.repositories.project.AuditFormOptionListRepository;
import com.devteam.aiauditserver.repositories.project.AuditFormTemplateRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Iterator;

/**
 * Idempotent seeder for the Level-1 firm-profile template, the Level-2 RICS
 * template, and the shared option lists. Driven entirely by JSON resources
 * under {@code knowledge_base/} — re-runs are no-ops once the templates
 * exist at the expected version.
 */
@Component
@Order(10)
public class TwoLevelTemplateSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(TwoLevelTemplateSeeder.class);

    private static final String OPTION_LISTS_PATH = "knowledge_base/level1_option_lists.v1.json";
    private static final String L1_PATH           = "knowledge_base/level1_questionnaire.v1.json";
    private static final String L2_PATH           = "knowledge_base/level2_rics_questions.v1.json";

    private final AuditFormTemplateRepository templateRepo;
    private final AuditFormOptionListRepository optionListRepo;
    private final ObjectMapper mapper = new ObjectMapper();

    public TwoLevelTemplateSeeder(AuditFormTemplateRepository templateRepo,
                                  AuditFormOptionListRepository optionListRepo) {
        this.templateRepo = templateRepo;
        this.optionListRepo = optionListRepo;
    }

    @Override
    @Transactional
    public void run(String... args) {
        try {
            seedOptionLists();
            seedTemplate(L1_PATH);
            seedTemplate(L2_PATH);
        } catch (Exception ex) {
            logger.error("TwoLevelTemplateSeeder failed", ex);
        }
    }

    // ── option lists ────────────────────────────────────────────────────

    private void seedOptionLists() throws IOException {
        JsonNode root = loadJson(OPTION_LISTS_PATH);
        for (JsonNode list : root.path("lists")) {
            String key = list.path("key").asText();
            if (key.isEmpty()) continue;
            String itemsJson = mapper.writeValueAsString(list.path("items"));
            AuditFormOptionList existing = optionListRepo.findByListKey(key).orElse(null);
            if (existing == null) {
                AuditFormOptionList created = new AuditFormOptionList();
                created.setListKey(key);
                created.setLabel(key);
                created.setItemsJson(itemsJson);
                optionListRepo.save(created);
                logger.info("Seeded option list '{}' ({} items)", key, list.path("items").size());
            } else if (!itemsJson.equals(existing.getItemsJson())) {
                existing.setItemsJson(itemsJson);
                existing.setUpdatedAt(new Date());
                optionListRepo.save(existing);
                logger.info("Updated option list '{}'", key);
            }
        }
    }

    // ── template (L1 or L2) ─────────────────────────────────────────────

    private void seedTemplate(String resourcePath) throws IOException {
        JsonNode root = loadJson(resourcePath);
        AuditType type = AuditType.valueOf(root.path("auditType").asText());
        AuditLevel level = AuditLevel.valueOf(root.path("level").asText());
        int version = root.path("version").asInt(1);
        int expectedSteps = root.path("steps").size();

        // Deactivate every stale template for this type+level whose version
        // is null (legacy DefaultTemplateLoaderService output) or whose step
        // count differs from the JSON. This rescues DBs polluted with the
        // 245-step RICS dump or duplicates from earlier dev runs.
        java.util.List<AuditFormTemplate> existingAll =
                templateRepo.findAllByAuditTypeAndLevel(type, level);
        AuditFormTemplate currentActive = null;
        for (AuditFormTemplate t : existingAll) {
            boolean legacy = t.getTemplateVersion() == null;
            boolean wrongShape = t.getSteps() != null && t.getSteps().size() != expectedSteps;
            if (legacy || wrongShape) {
                if (Boolean.TRUE.equals(t.getActive())) {
                    t.setActive(false);
                    templateRepo.save(t);
                    logger.warn("Deactivated stale {}/{} template id={} (version={}, steps={})",
                            type, level, t.getId(), t.getTemplateVersion(),
                            t.getSteps() == null ? 0 : t.getSteps().size());
                }
                continue;
            }
            if (Boolean.TRUE.equals(t.getActive()) &&
                    (currentActive == null
                            || (t.getTemplateVersion() != null
                                && t.getTemplateVersion() > currentActive.getTemplateVersion()))) {
                currentActive = t;
            }
        }

        if (currentActive != null && currentActive.getTemplateVersion() != null
                && currentActive.getTemplateVersion() >= version) {
            logger.info("Template {}/{} already at version {} — skipping seed",
                    type, level, currentActive.getTemplateVersion());
            return;
        }
        if (currentActive != null) {
            currentActive.setActive(false);
            templateRepo.save(currentActive);
            logger.info("Deactivated template id={} (v{}) before seeding v{}",
                    currentActive.getId(), currentActive.getTemplateVersion(), version);
        }

        // Avoid colliding with the (type, level, version) unique constraint:
        // pick a version strictly greater than every row already in the DB
        // (active or not), legacy nulls treated as 0.
        int maxExistingVersion = 0;
        for (AuditFormTemplate t : existingAll) {
            int v = t.getTemplateVersion() == null ? 0 : t.getTemplateVersion();
            if (v > maxExistingVersion) maxExistingVersion = v;
        }
        int effectiveVersion = Math.max(version, maxExistingVersion + 1);
        if (effectiveVersion != version) {
            logger.info("Bumping new {}/{} template version from {} to {} to avoid uk_template_type_level_version collision",
                    type, level, version, effectiveVersion);
        }

        AuditFormTemplate template = new AuditFormTemplate();
        template.setAuditType(type);
        template.setLevel(level);
        template.setTemplateVersion(effectiveVersion);
        template.setTitle(root.path("title").asText());
        template.setDescription(root.path("description").asText(""));
        template.setActive(true);

        // Build the level-2 shared response options inline if present.
        JsonNode l2Options = root.path("responseOptions");
        if (l2Options.isArray() && l2Options.size() > 0
                && !optionListRepo.existsByListKey("L2_Response")) {
            AuditFormOptionList l2Opts = new AuditFormOptionList();
            l2Opts.setListKey("L2_Response");
            l2Opts.setLabel("L2_Response");
            l2Opts.setItemsJson(mapper.writeValueAsString(l2Options));
            optionListRepo.save(l2Opts);
            logger.info("Seeded option list 'L2_Response' ({} items)", l2Options.size());
        }

        for (JsonNode stepNode : root.path("steps")) {
            AuditFormStep step = new AuditFormStep();
            step.setTemplate(template);
            step.setStepOrder(stepNode.path("stepOrder").asInt());
            step.setTitle(stepNode.path("title").asText());
            step.setDescription(stepNode.path("description").asText(""));

            for (JsonNode fieldNode : stepNode.path("fields")) {
                AuditFormField field = buildField(step, fieldNode);
                step.getFields().add(field);
            }
            template.getSteps().add(step);
        }

        AuditFormTemplate saved = templateRepo.save(template);
        logger.info("Seeded template {}/{} v{} id={} with {} steps",
                type, level, version, saved.getId(), saved.getSteps().size());
    }

    private AuditFormField buildField(AuditFormStep step, JsonNode node) {
        AuditFormField f = new AuditFormField();
        f.setStep(step);
        f.setFieldOrder(node.path("fieldOrder").asInt());
        f.setLabel(node.path("label").asText());
        f.setPlaceholder(textOrNull(node, "placeholder"));
        f.setFieldType(FieldType.valueOf(node.path("fieldType").asText("TEXT")));
        f.setRequired(node.path("required").asBoolean(false));
        f.setMultipleFiles(node.path("multipleFiles").asBoolean(false));
        f.setFieldKey(textOrNull(node, "fieldKey"));
        f.setOptionSourceKey(textOrNull(node, "optionSourceKey"));

        f.setVisibilityRule(jsonOrNull(node, "visibilityRule"));
        f.setRoutingTags(jsonOrNull(node, "routingTags"));

        f.setRicsClause(textOrNull(node, "ricsClause"));
        f.setModule(textOrNull(node, "module"));
        f.setCategory(textOrNull(node, "category"));
        f.setApplicabilityTrigger(textOrNull(node, "applicabilityTrigger"));
        f.setEvidenceDepth(textOrNull(node, "evidenceDepth"));
        f.setPriority(textOrNull(node, "priority"));
        f.setExpectedEvidence(textOrNull(node, "expectedEvidence"));
        f.setRationale(textOrNull(node, "rationale"));

        // For RADIO/DROPDOWN/MULTI_CHECKBOX, materialise options from the
        // shared list (so the existing UI keeps working without extra calls).
        String optKey = f.getOptionSourceKey();
        if (optKey != null && !optKey.isEmpty()) {
            try {
                AuditFormOptionList list = optionListRepo.findByListKey(optKey).orElse(null);
                if (list != null) {
                    JsonNode items = mapper.readTree(list.getItemsJson());
                    int idx = 0;
                    for (JsonNode it : items) {
                        AuditFormFieldOption opt = new AuditFormFieldOption();
                        opt.setField(f);
                        opt.setLabel(it.path("label").asText());
                        opt.setValue(it.path("value").asText());
                        opt.setOptionOrder(idx++);
                        f.getOptions().add(opt);
                    }
                }
            } catch (IOException ignored) {}
        }
        return f;
    }

    // ── helpers ─────────────────────────────────────────────────────────

    private JsonNode loadJson(String path) throws IOException {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return mapper.readTree(in);
        }
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode n = node.get(field);
        if (n == null || n.isNull()) return null;
        String s = n.isTextual() ? n.asText() : n.toString();
        return s.isEmpty() ? null : s;
    }

    private String jsonOrNull(JsonNode node, String field) {
        JsonNode n = node.get(field);
        if (n == null || n.isNull()) return null;
        if (n.isTextual()) return n.asText();
        try { return mapper.writeValueAsString(n); }
        catch (Exception e) { return null; }
    }

    @SuppressWarnings("unused")
    private static <T> Iterable<T> iter(Iterator<T> it) {
        return () -> it;
    }
}

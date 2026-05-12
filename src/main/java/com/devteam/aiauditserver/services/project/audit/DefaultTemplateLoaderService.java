package com.devteam.aiauditserver.services.project.audit;

import com.devteam.aiauditserver.enums.Project.AuditType;
import com.devteam.aiauditserver.enums.Project.FieldType;
import com.devteam.aiauditserver.models.project.AuditForm.AuditFormField;
import com.devteam.aiauditserver.models.project.AuditForm.AuditFormFieldOption;
import com.devteam.aiauditserver.models.project.AuditForm.AuditFormStep;
import com.devteam.aiauditserver.models.project.AuditForm.AuditFormTemplate;
import com.devteam.aiauditserver.repositories.project.AuditFormTemplateRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds default audit form templates from knowledge-base JSON files
 * on the Spring classpath ({@code /knowledge_base/}).
 *
 * Currently only RICS Responsible AI is wired; the structure of this class
 * is intentionally generic so other audit types can be added later.
 */
@Service
public class DefaultTemplateLoaderService {

    private static final Logger logger =
            LoggerFactory.getLogger(DefaultTemplateLoaderService.class);

    private static final String RICS_KB_PATH = "knowledge_base/rics_knowledge_base.json";
    private static final String RICS_TEMPLATE_TITLE = "RICS Responsible AI Review";
    private static final String RICS_TEMPLATE_DESCRIPTION =
            "Default review based on the RICS Professional Standard "
            + "'Responsible use of artificial intelligence in surveying practice' "
            + "(1st edition, September 2025).";

    @Autowired
    private AuditFormTemplateRepository templateRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Generates the default RICS Responsible AI template from the
     * RICS knowledge base. If a RICS template already exists, it is
     * deleted first (regenerate semantics). Fails with a clear message
     * if the existing template cannot be removed (e.g. linked to an
     * in-flight audit request via foreign keys).
     */
    @Transactional
    public AuditFormTemplate generateDefaultRicsTemplate() {
        templateRepository.findByAuditType(AuditType.RICS_RESPONSIBLE_AI)
                .ifPresent(existing -> {
                    try {
                        templateRepository.delete(existing);
                        templateRepository.flush();
                        logger.info("Deleted existing RICS template id={} before regenerate",
                                existing.getId());
                    } catch (Exception ex) {
                        throw new IllegalStateException(
                                "An existing RICS template (id=" + existing.getId()
                                + ") is in use by audit requests and cannot be replaced. "
                                + "Deactivate it or remove dependent requests first.", ex);
                    }
                });

        JsonNode root = loadJson(RICS_KB_PATH);
        JsonNode rules = root.path("rules");
        if (!rules.isArray() || rules.size() == 0) {
            throw new IllegalStateException("RICS knowledge base has no rules");
        }

        AuditFormTemplate template = new AuditFormTemplate();
        template.setAuditType(AuditType.RICS_RESPONSIBLE_AI);
        template.setTitle(RICS_TEMPLATE_TITLE);
        template.setDescription(RICS_TEMPLATE_DESCRIPTION);
        template.setActive(true);

        // Group rules by section (preserve first-seen order).
        Map<String, List<JsonNode>> bySection = new LinkedHashMap<>();
        Map<String, String> sectionTitles = new LinkedHashMap<>();
        for (JsonNode rule : rules) {
            String section = rule.path("section").asText("");
            String sectionTitle = rule.path("section_title").asText(section);
            bySection.computeIfAbsent(section, k -> new ArrayList<>()).add(rule);
            sectionTitles.putIfAbsent(section, sectionTitle);
        }

        int stepOrder = 1;
        for (Map.Entry<String, List<JsonNode>> e : bySection.entrySet()) {
            AuditFormStep step = buildSectionStep(
                    template,
                    sectionTitles.get(e.getKey()),
                    e.getValue(),
                    stepOrder++);
            template.getSteps().add(step);
        }

        AuditFormTemplate saved = templateRepository.save(template);
        logger.info("Generated default RICS template id={} with {} step(s)",
                saved.getId(), saved.getSteps().size());
        return saved;
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private AuditFormStep buildSectionStep(AuditFormTemplate template,
                                           String sectionTitle,
                                           List<JsonNode> rulesInSection,
                                           int stepOrder) {
        AuditFormStep step = new AuditFormStep();
        step.setTemplate(template);
        step.setTitle(sectionTitle);
        step.setDescription("Tell us how your organization addresses "
                + sectionTitle.toLowerCase() + " in your AI practice. "
                + "Answer each item and attach any supporting evidence.");
        step.setStepOrder(stepOrder);

        int fieldOrder = 1;
        for (JsonNode rule : rulesInSection) {
            String ruleId = rule.path("id").asText("");
            String requirement = rule.path("requirement_text").asText("");
            String evidenceImplied = rule.path("evidence_implied").asText("");

            // 1) Self-assessment question (Radio: Yes / Partially / No / N/A)
            AuditFormField question = new AuditFormField();
            question.setStep(step);
            question.setLabel("Does your organization meet this requirement? "
                    + requirement);
            question.setPlaceholder(null);
            question.setFieldType(FieldType.RADIO);
            question.setRequired(true);
            question.setMultipleFiles(false);
            question.setFieldOrder(fieldOrder++);
            question.getOptions().add(option(question, "Yes",       "YES",       0));
            question.getOptions().add(option(question, "Partially", "PARTIALLY", 1));
            question.getOptions().add(option(question, "No",        "NO",        2));
            question.getOptions().add(option(question, "Not applicable", "NA",   3));
            step.getFields().add(question);

            // 2) Optional comment / evidence note
            AuditFormField comment = new AuditFormField();
            comment.setStep(step);
            comment.setLabel("Describe how this is applied in your organization");
            comment.setPlaceholder(evidenceImplied.isEmpty()
                    ? "Explain your current practice and reference any supporting controls or documents."
                    : "Suggested evidence: " + evidenceImplied);
            comment.setFieldType(FieldType.TEXTAREA);
            comment.setRequired(false);
            comment.setMultipleFiles(false);
            comment.setFieldOrder(fieldOrder++);
            step.getFields().add(comment);
        }

        // 3) Section-wide multi-file evidence upload
        AuditFormField evidence = new AuditFormField();
        evidence.setStep(step);
        evidence.setLabel("Evidence documents for " + sectionTitle);
        evidence.setPlaceholder("Upload policies, registers, logs or other supporting documents.");
        evidence.setFieldType(FieldType.FILE);
        evidence.setRequired(false);
        evidence.setMultipleFiles(true);
        evidence.setFieldOrder(fieldOrder);
        step.getFields().add(evidence);

        return step;
    }

    private AuditFormFieldOption option(AuditFormField field,
                                        String label,
                                        String value,
                                        int order) {
        AuditFormFieldOption opt = new AuditFormFieldOption();
        opt.setField(field);
        opt.setLabel(label);
        opt.setValue(value);
        opt.setOptionOrder(order);
        return opt;
    }

    private JsonNode loadJson(String classpathLocation) {
        try (InputStream in = new ClassPathResource(classpathLocation).getInputStream()) {
            return objectMapper.readTree(in);
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Could not load knowledge base from classpath: " + classpathLocation, ex);
        }
    }
}

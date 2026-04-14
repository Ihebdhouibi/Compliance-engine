package com.devteam.aiauditserver.services.project.audit;


import com.devteam.aiauditserver.enums.Project.AuditType;
import com.devteam.aiauditserver.models.project.AuditForm.AuditFormField;
import com.devteam.aiauditserver.models.project.AuditForm.AuditFormFieldOption;
import com.devteam.aiauditserver.models.project.AuditForm.AuditFormStep;
import com.devteam.aiauditserver.models.project.AuditForm.AuditFormTemplate;
import com.devteam.aiauditserver.repositories.project.AuditFormFieldRepository;
import com.devteam.aiauditserver.repositories.project.AuditFormStepRepository;
import com.devteam.aiauditserver.repositories.project.AuditFormTemplateRepository;
import com.devteam.aiauditserver.requests.project.AddFieldRequest;
import com.devteam.aiauditserver.requests.project.AddStepRequest;
import com.devteam.aiauditserver.requests.project.CreateAuditFormTemplateRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditFormTemplateService {

    @Autowired
    private AuditFormTemplateRepository templateRepository;

    @Autowired
    private AuditFormStepRepository stepRepository;

    @Autowired
    private AuditFormFieldRepository fieldRepository;

    // ── Get all active templates
    public List<AuditFormTemplate> getAllTemplates() {
        return templateRepository.findByActiveTrue();
    }

    // ── Get template by audit type
    public AuditFormTemplate getByAuditType(AuditType auditType) {
        return templateRepository.findByAuditType(auditType)
                .orElseThrow(() -> new RuntimeException(
                        "No form template found for audit type: " + auditType));
    }

    // ── Get template by id
    public AuditFormTemplate getById(Long id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found"));
    }

    // ── Create new template
    public AuditFormTemplate createTemplate(CreateAuditFormTemplateRequest req) {
        if (templateRepository.existsByAuditType(req.getAuditType())) {
            throw new RuntimeException(
                    "A template for " + req.getAuditType() + " already exists");
        }
        AuditFormTemplate template = new AuditFormTemplate();
        template.setAuditType(req.getAuditType());
        template.setTitle(req.getTitle());
        template.setDescription(req.getDescription());
        return templateRepository.save(template);
    }

    // ── Add step to template
    public AuditFormStep addStep(Long templateId, AddStepRequest req) {
        AuditFormTemplate template = getById(templateId);

        AuditFormStep step = new AuditFormStep();
        step.setTemplate(template);
        step.setTitle(req.getTitle());
        step.setDescription(req.getDescription());

        // Auto-assign step order if not provided
        int order = req.getStepOrder() != null
                ? req.getStepOrder()
                : template.getSteps().size() + 1;
        step.setStepOrder(order);

        return stepRepository.save(step);
    }

    // ── Remove step
    public void removeStep(Long stepId) {
        stepRepository.deleteById(stepId);
    }

    // ── Add field to step
    public AuditFormField addField(Long stepId, AddFieldRequest req) {
        AuditFormStep step = stepRepository.findById(stepId)
                .orElseThrow(() -> new RuntimeException("Step not found"));

        AuditFormField field = new AuditFormField();
        field.setStep(step);
        field.setLabel(req.getLabel());
        field.setPlaceholder(req.getPlaceholder());
        field.setFieldType(req.getFieldType());
        field.setRequired(req.getRequired() != null ? req.getRequired() : false);

        int order = req.getFieldOrder() != null
                ? req.getFieldOrder()
                : step.getFields().size() + 1;
        field.setFieldOrder(order);

        // Add options for choice-type fields
        if (req.getOptions() != null && !req.getOptions().isEmpty()) {
            int i = 0;
            for (AddFieldRequest.FieldOptionRequest opt : req.getOptions()) {
                AuditFormFieldOption option = new AuditFormFieldOption();
                option.setLabel(opt.getLabel());
                option.setValue(opt.getValue());
                option.setOptionOrder(opt.getOptionOrder() != null ? opt.getOptionOrder() : i++);
                option.setField(field);
                field.getOptions().add(option);
            }
        }

        return fieldRepository.save(field);
    }

    // ── Remove field
    public void removeField(Long fieldId) {
        fieldRepository.deleteById(fieldId);
    }

    // ── Toggle template active/inactive
    public AuditFormTemplate toggleActive(Long templateId) {
        AuditFormTemplate template = getById(templateId);
        template.setActive(!template.getActive());
        return templateRepository.save(template);
    }
}
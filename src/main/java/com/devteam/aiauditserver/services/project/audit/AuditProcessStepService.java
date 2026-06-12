package com.devteam.aiauditserver.services.project.audit;


import com.devteam.aiauditserver.models.project.AuditForm.AuditFormTemplate;
import com.devteam.aiauditserver.models.project.AuditRequest.AuditProcessStep;
import com.devteam.aiauditserver.repositories.project.AuditFormTemplateRepository;
import com.devteam.aiauditserver.repositories.project.AuditProcessStepRepository;
import com.devteam.aiauditserver.requests.project.CreateAuditProcessStepRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import com.devteam.aiauditserver.models.project.AuditForm.AuditFormTemplate;
import com.devteam.aiauditserver.models.project.AuditRequest.AuditProcessStep;
import com.devteam.aiauditserver.repositories.project.AuditFormTemplateRepository;
import com.devteam.aiauditserver.repositories.project.AuditProcessStepRepository;
import com.devteam.aiauditserver.requests.project.CreateAuditProcessStepRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditProcessStepService {

    @Autowired
    private AuditProcessStepRepository stepRepo;

    @Autowired
    private AuditFormTemplateRepository templateRepo;

    // Returns real steps if any exist, otherwise a virtual default (not persisted)
    public List<AuditProcessStep> getStepsForTemplate(Long templateId) {
        List<AuditProcessStep> steps =
                stepRepo.findByTemplateIdOrderByStepOrderAsc(templateId);
        if (steps.isEmpty()) {
            return List.of(buildVirtualDefault());
        }
        return steps;
    }

    public AuditProcessStep addStep(Long templateId,
                                    CreateAuditProcessStepRequest req) {
        AuditFormTemplate template = templateRepo.findById(templateId)
                .orElseThrow(() ->
                        new RuntimeException("Template not found: " + templateId));

        long count = stepRepo.countByTemplateId(templateId);

        AuditProcessStep step = new AuditProcessStep();
        step.setName(req.getName());
        step.setStepOrder(req.getStepOrder() != null
                ? req.getStepOrder()
                : (int)(count + 1));
        step.setTemplate(template);
        step.setIsDefault(false);
        return stepRepo.save(step);
    }

    public AuditProcessStep updateStep(Long stepId,
                                       CreateAuditProcessStepRequest req) {
        AuditProcessStep step = stepRepo.findById(stepId)
                .orElseThrow(() ->
                        new RuntimeException("ProcessStep not found: " + stepId));

        if (req.getName() != null && !req.getName().isBlank()) {
            step.setName(req.getName());
        }
        if (req.getStepOrder() != null) {
            step.setStepOrder(req.getStepOrder());
        }
        return stepRepo.save(step);
    }

    // Removes step from template only.
    // AuditStepResult rows retain stepName snapshot — historical data safe.
    // Their processStep FK column becomes null automatically (nullable = true).
    public void deleteStep(Long stepId) {
        stepRepo.deleteById(stepId);
    }

    private AuditProcessStep buildVirtualDefault() {
        AuditProcessStep def = new AuditProcessStep();
        def.setId(-1L);
        def.setName("Audit Review");
        def.setStepOrder(1);
        def.setIsDefault(true);
        return def;
    }
}

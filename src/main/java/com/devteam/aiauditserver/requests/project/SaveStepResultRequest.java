package com.devteam.aiauditserver.requests.project;



public class SaveStepResultRequest {

    // The process step ID from template (-1 or null = default virtual step)
    private Long processStepId;

    // Name snapshot sent from frontend — stored even if step deleted later
    private String stepName;

    private String description;

    // "DRAFT" or "SAVED"
    private String status;

    public Long getProcessStepId() { return processStepId; }
    public void setProcessStepId(Long processStepId) {
        this.processStepId = processStepId;
    }

    public String getStepName() { return stepName; }
    public void setStepName(String stepName) { this.stepName = stepName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
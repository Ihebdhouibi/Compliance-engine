package com.devteam.aiauditserver.requests.project;


import com.devteam.aiauditserver.enums.Project.AuditType;

public class CreateAuditFormTemplateRequest {
    private AuditType auditType;
    private String title;
    private String description;

    public AuditType getAuditType() { return auditType; }
    public void setAuditType(AuditType auditType) { this.auditType = auditType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
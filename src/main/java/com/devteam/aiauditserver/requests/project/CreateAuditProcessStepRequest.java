package com.devteam.aiauditserver.requests.project;

public class CreateAuditProcessStepRequest {
    private String name;
    private Integer stepOrder;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getStepOrder() { return stepOrder; }
    public void setStepOrder(Integer stepOrder) { this.stepOrder = stepOrder; }
}
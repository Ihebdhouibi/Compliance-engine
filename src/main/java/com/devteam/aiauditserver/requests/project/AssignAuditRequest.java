package com.devteam.aiauditserver.requests.project;


import java.util.Date;

public class AssignAuditRequest {
    private Long assignedToUserId;
    private Date dueDate;

    public Long getAssignedToUserId() { return assignedToUserId; }
    public void setAssignedToUserId(Long assignedToUserId) { this.assignedToUserId = assignedToUserId; }

    public Date getDueDate() { return dueDate; }
    public void setDueDate(Date dueDate) { this.dueDate = dueDate; }
}

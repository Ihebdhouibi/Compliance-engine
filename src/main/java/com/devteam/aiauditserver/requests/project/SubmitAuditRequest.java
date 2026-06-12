package com.devteam.aiauditserver.requests.project;

import com.devteam.aiauditserver.enums.Project.AuditType;

import java.util.List;

public class SubmitAuditRequest {
    private AuditType auditType;
    private List<AnswerRequest> answers;

    public static class AnswerRequest {
        private Long fieldId;
        private String fieldLabel;
        private String answerValue;

        public Long getFieldId() { return fieldId; }
        public void setFieldId(Long fieldId) { this.fieldId = fieldId; }

        public String getFieldLabel() { return fieldLabel; }
        public void setFieldLabel(String fieldLabel) { this.fieldLabel = fieldLabel; }

        public String getAnswerValue() { return answerValue; }
        public void setAnswerValue(String answerValue) { this.answerValue = answerValue; }
    }

    public AuditType getAuditType() { return auditType; }
    public void setAuditType(AuditType auditType) { this.auditType = auditType; }

    public List<AnswerRequest> getAnswers() { return answers; }
    public void setAnswers(List<AnswerRequest> answers) { this.answers = answers; }
}

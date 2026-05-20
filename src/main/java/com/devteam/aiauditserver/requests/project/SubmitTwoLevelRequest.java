package com.devteam.aiauditserver.requests.project;

import java.util.List;

/** Request payload for the L1-aware submit endpoint. */
public class SubmitTwoLevelRequest {

    /** Optional — if omitted, computed from the selector_org_type field. */
    private String auditType;
    private List<TwoLevelAnswer> answers;

    public String getAuditType() { return auditType; }
    public void setAuditType(String auditType) { this.auditType = auditType; }

    public List<TwoLevelAnswer> getAnswers() { return answers; }
    public void setAnswers(List<TwoLevelAnswer> answers) { this.answers = answers; }

    public static class TwoLevelAnswer {
        private Long fieldId;
        private String fieldKey;
        private String fieldLabel;
        private String answerValue;

        public Long getFieldId() { return fieldId; }
        public void setFieldId(Long fieldId) { this.fieldId = fieldId; }

        public String getFieldKey() { return fieldKey; }
        public void setFieldKey(String fieldKey) { this.fieldKey = fieldKey; }

        public String getFieldLabel() { return fieldLabel; }
        public void setFieldLabel(String fieldLabel) { this.fieldLabel = fieldLabel; }

        public String getAnswerValue() { return answerValue; }
        public void setAnswerValue(String answerValue) { this.answerValue = answerValue; }
    }
}

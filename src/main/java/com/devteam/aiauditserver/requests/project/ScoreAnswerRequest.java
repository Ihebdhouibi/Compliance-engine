package com.devteam.aiauditserver.requests.project;

/** Auditor scoring payload for a single Level-2 answer. */
public class ScoreAnswerRequest {
    private Integer auditorScore;
    private String auditorNotes;
    private String evidenceProvidedStatus;
    private String evidenceReference;

    public Integer getAuditorScore() { return auditorScore; }
    public void setAuditorScore(Integer auditorScore) { this.auditorScore = auditorScore; }

    public String getAuditorNotes() { return auditorNotes; }
    public void setAuditorNotes(String auditorNotes) { this.auditorNotes = auditorNotes; }

    public String getEvidenceProvidedStatus() { return evidenceProvidedStatus; }
    public void setEvidenceProvidedStatus(String evidenceProvidedStatus) {
        this.evidenceProvidedStatus = evidenceProvidedStatus;
    }

    public String getEvidenceReference() { return evidenceReference; }
    public void setEvidenceReference(String evidenceReference) { this.evidenceReference = evidenceReference; }
}

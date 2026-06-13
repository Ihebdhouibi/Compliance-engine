package com.devteam.aiauditserver.requests.project;


public class SaveChatMessageRequest {

    // "USER" or "ASSISTANT"
    private String role;

    private String content;

    // Optional JSON-encoded RAG sources for an assistant reply
    private String sources;

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getSources() { return sources; }
    public void setSources(String sources) { this.sources = sources; }
}

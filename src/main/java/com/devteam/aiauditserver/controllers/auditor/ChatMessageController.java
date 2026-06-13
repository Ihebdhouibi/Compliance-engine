package com.devteam.aiauditserver.controllers.auditor;


import com.devteam.aiauditserver.Tools.util.BaseController;
import com.devteam.aiauditserver.models.project.AuditRequest.ChatMessage;
import com.devteam.aiauditserver.requests.project.SaveChatMessageRequest;
import com.devteam.aiauditserver.services.project.audit.ChatMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Persistent transcript for the AI Audit Assistant, scoped to one audit
 * request so the conversation is restored whenever the audit is reopened.
 */
@RestController
@CrossOrigin
@RequestMapping("/api/v1/audit-requests/{requestId}/chat-messages")
public class ChatMessageController extends BaseController {

    @Autowired
    private ChatMessageService chatService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR', 'USER')")
    public ResponseEntity<List<ChatMessage>> getMessages(
            @PathVariable Long requestId) {
        return ResponseEntity.ok(
                chatService.getMessagesForRequest(requestId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR', 'USER')")
    public ResponseEntity<ChatMessage> save(
            @PathVariable Long requestId,
            @RequestBody SaveChatMessageRequest req) {
        String username = getCurrentUser().getUsername();
        return ResponseEntity.ok(
                chatService.saveMessage(requestId, req, username));
    }

    @DeleteMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR', 'USER')")
    public ResponseEntity<Void> clear(@PathVariable Long requestId) {
        chatService.clearMessages(requestId);
        return ResponseEntity.noContent().build();
    }
}

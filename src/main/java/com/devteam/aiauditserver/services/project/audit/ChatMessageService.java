package com.devteam.aiauditserver.services.project.audit;


import com.devteam.aiauditserver.models.User.User;
import com.devteam.aiauditserver.models.project.AuditRequest.AuditRequest;
import com.devteam.aiauditserver.models.project.AuditRequest.ChatMessage;
import com.devteam.aiauditserver.repositories.project.AuditRequestRepository;
import com.devteam.aiauditserver.repositories.project.ChatMessageRepository;
import com.devteam.aiauditserver.requests.project.SaveChatMessageRequest;
import com.devteam.aiauditserver.services.auth.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChatMessageService {

    @Autowired private ChatMessageRepository chatRepo;
    @Autowired private AuditRequestRepository requestRepo;
    @Autowired private UserService userService;

    public List<ChatMessage> getMessagesForRequest(Long requestId) {
        return chatRepo.findByAuditRequestIdOrderByCreatedAtAsc(requestId);
    }

    public ChatMessage saveMessage(Long requestId,
                                   SaveChatMessageRequest req,
                                   String username) {
        AuditRequest auditRequest = requestRepo.findById(requestId)
                .orElseThrow(() ->
                        new RuntimeException("AuditRequest not found: " + requestId));

        ChatMessage message = new ChatMessage();
        message.setAuditRequest(auditRequest);
        message.setRole(parseRole(req.getRole()));
        message.setContent(req.getContent());
        message.setSources(req.getSources());

        if (username != null) {
            try {
                User user = userService.findByUserName(username);
                message.setAuthor(user);
            } catch (RuntimeException ignored) {
                // author is optional — keep the transcript even if the user
                // record cannot be resolved
            }
        }

        return chatRepo.save(message);
    }

    @Transactional
    public void clearMessages(Long requestId) {
        chatRepo.deleteByAuditRequestId(requestId);
    }

    private ChatMessage.MessageRole parseRole(String r) {
        if ("ASSISTANT".equalsIgnoreCase(r))
            return ChatMessage.MessageRole.ASSISTANT;
        return ChatMessage.MessageRole.USER;
    }
}

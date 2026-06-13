package com.devteam.aiauditserver.repositories.project;

import com.devteam.aiauditserver.models.project.AuditRequest.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByAuditRequestIdOrderByCreatedAtAsc(Long requestId);
    void deleteByAuditRequestId(Long requestId);
}

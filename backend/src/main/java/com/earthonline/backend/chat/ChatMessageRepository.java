package com.earthonline.backend.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

    @Query("""
            select message from ChatMessageEntity message
            where (message.sender.id = :currentUserId and message.receiver.id = :friendUserId)
               or (message.sender.id = :friendUserId and message.receiver.id = :currentUserId)
            order by message.createdAt asc, message.id asc
            """)
    List<ChatMessageEntity> findConversation(
            @Param("currentUserId") Long currentUserId,
            @Param("friendUserId") Long friendUserId
    );
}

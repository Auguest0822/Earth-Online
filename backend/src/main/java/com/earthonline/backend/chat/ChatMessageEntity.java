package com.earthonline.backend.chat;

import com.earthonline.backend.user.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages", uniqueConstraints = {
        @UniqueConstraint(name = "uk_chat_sender_client_message", columnNames = {"sender_id", "client_message_id"})
}, indexes = {
        @Index(name = "idx_chat_messages_sender", columnList = "sender_id"),
        @Index(name = "idx_chat_messages_receiver", columnList = "receiver_id"),
        @Index(name = "idx_chat_messages_created", columnList = "created_at")
})
public class ChatMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private UserEntity sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_id", nullable = false)
    private UserEntity receiver;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(name = "client_message_id", length = 64)
    private String clientMessageId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    protected ChatMessageEntity() {
    }

    public ChatMessageEntity(UserEntity sender, UserEntity receiver, String content, String clientMessageId) {
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.clientMessageId = clientMessageId;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public UserEntity getSender() { return sender; }
    public UserEntity getReceiver() { return receiver; }
    public String getContent() { return content; }
    public String getClientMessageId() { return clientMessageId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getReadAt() { return readAt; }

    public void markRead(LocalDateTime readAt) {
        if (this.readAt == null) this.readAt = readAt;
    }
}

package com.earthonline.backend.chat;

import com.earthonline.backend.chat.dto.ChatMessageResponse;
import com.earthonline.backend.chat.dto.ChatReadReceiptResponse;
import com.earthonline.backend.chat.dto.ChatUnreadCountResponse;
import com.earthonline.backend.chat.dto.ChatUserResponse;
import com.earthonline.backend.chat.dto.SendChatMessageRequest;
import com.earthonline.backend.friends.FriendRequestRepository;
import com.earthonline.backend.friends.FriendRequestStatus;
import com.earthonline.backend.storage.StorageService;
import com.earthonline.backend.user.UserEntity;
import com.earthonline.backend.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final StorageService storageService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatService(
            ChatMessageRepository chatMessageRepository,
            UserRepository userRepository,
            FriendRequestRepository friendRequestRepository,
            StorageService storageService,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
        this.friendRequestRepository = friendRequestRepository;
        this.storageService = storageService;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getConversation(String currentUsername, String friendPublicId) {
        UserEntity currentUser = findUserByUsername(currentUsername);
        UserEntity friend = findUserByPublicId(friendPublicId);
        requireFriendship(currentUser, friend);

        return chatMessageRepository.findConversation(currentUser.getId(), friend.getId())
                .stream()
                .map(message -> toResponse(message, currentUser.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChatUnreadCountResponse> getUnreadCounts(String currentUsername) {
        UserEntity currentUser = findUserByUsername(currentUsername);
        Map<String, Long> counts = chatMessageRepository
                .findByReceiverIdAndReadAtIsNull(currentUser.getId())
                .stream()
                .collect(Collectors.groupingBy(
                        message -> message.getSender().getPublicId(),
                        Collectors.counting()
                ));
        return counts.entrySet().stream()
                .map(entry -> new ChatUnreadCountResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    @Transactional
    public ChatReadReceiptResponse markConversationRead(String currentUsername, String friendPublicId) {
        UserEntity currentUser = findUserByUsername(currentUsername);
        UserEntity friend = findUserByPublicId(friendPublicId);
        requireFriendship(currentUser, friend);

        List<ChatMessageEntity> unreadMessages = chatMessageRepository
                .findBySenderIdAndReceiverIdAndReadAtIsNull(friend.getId(), currentUser.getId());
        LocalDateTime readAt = LocalDateTime.now();
        unreadMessages.forEach(message -> message.markRead(readAt));
        Long lastReadMessageId = unreadMessages.stream()
                .map(ChatMessageEntity::getId)
                .max(Long::compareTo)
                .orElse(null);

        ChatReadReceiptResponse receipt = new ChatReadReceiptResponse(
                currentUser.getPublicId(), lastReadMessageId, readAt
        );
        if (lastReadMessageId != null) {
            messagingTemplate.convertAndSendToUser(
                    friend.getUsername(),
                    "/queue/chat/read-receipts",
                    receipt
            );
        }
        return receipt;
    }

    /**
     * REST fallback: saves the message and returns it to the sender.
     */
    @Transactional
    public ChatMessageResponse sendMessage(String currentUsername, SendChatMessageRequest request) {
        ChatDelivery delivery = saveMessage(currentUsername, request);
        if (delivery.newlyCreated()) pushToReceiver(delivery);
        return delivery.senderResponse();
    }

    /**
     * WebSocket path: saves the message, then pushes authoritative copies to
     * both participants' private STOMP destinations.
     */
    @Transactional
    public ChatMessageResponse sendMessageAndPush(String currentUsername, SendChatMessageRequest request) {
        ChatDelivery delivery = saveMessage(currentUsername, request);
        if (delivery.newlyCreated()) pushToReceiver(delivery);
        messagingTemplate.convertAndSendToUser(
                currentUsername,
                "/queue/chat/messages",
                delivery.senderResponse()
        );
        return delivery.senderResponse();
    }

    private void pushToReceiver(ChatDelivery delivery) {
        messagingTemplate.convertAndSendToUser(
                delivery.receiverUsername(),
                "/queue/chat/messages",
                delivery.receiverResponse()
        );
    }

    private ChatDelivery saveMessage(String currentUsername, SendChatMessageRequest request) {
        UserEntity sender = findUserByUsername(currentUsername);
        UserEntity receiver = findUserByPublicId(request.recipientPublicId());
        requireFriendship(sender, receiver);

        String clientMessageId = request.clientMessageId() == null
                ? null
                : request.clientMessageId().trim();
        if (clientMessageId != null && !clientMessageId.isEmpty()) {
            ChatMessageEntity existing = chatMessageRepository
                    .findBySenderIdAndClientMessageId(sender.getId(), clientMessageId)
                    .orElse(null);
            if (existing != null) {
                if (!existing.getReceiver().getId().equals(receiver.getId())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Client message ID already used");
                }
                return new ChatDelivery(
                        receiver.getUsername(),
                        toResponse(existing, sender.getId()),
                        toResponse(existing, receiver.getId()),
                        false
                );
            }
        }

        ChatMessageEntity message = chatMessageRepository.save(
                new ChatMessageEntity(sender, receiver, request.content().trim(), clientMessageId)
        );

        return new ChatDelivery(
                receiver.getUsername(),
                toResponse(message, sender.getId()),
                toResponse(message, receiver.getId()),
                true
        );
    }

    private void requireFriendship(UserEntity currentUser, UserEntity friend) {
        if (currentUser.getId().equals(friend.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot message yourself");
        }

        boolean friends = friendRequestRepository
                .findRelationshipsBetween(currentUser.getId(), friend.getId())
                .stream()
                .anyMatch(request -> request.getStatus() == FriendRequestStatus.ACCEPTED);
        if (!friends) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only friends can chat");
        }
    }

    private UserEntity findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid login"));
    }

    private UserEntity findUserByPublicId(String publicId) {
        return userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private ChatMessageResponse toResponse(ChatMessageEntity message, Long currentUserId) {
        UserEntity sender = message.getSender();
        UserEntity receiver = message.getReceiver();
        return new ChatMessageResponse(
                message.getId(),
                new ChatUserResponse(
                        sender.getPublicId(),
                        sender.getUsername(),
                        storageService.getAvatarPreviewUrl(sender.getUsername(), sender.getAvatar())
                ),
                new ChatUserResponse(
                        receiver.getPublicId(),
                        receiver.getUsername(),
                        storageService.getAvatarPreviewUrl(receiver.getUsername(), receiver.getAvatar())
                ),
                message.getContent(),
                message.getClientMessageId(),
                message.getCreatedAt(),
                message.getReadAt(),
                sender.getId().equals(currentUserId)
        );
    }

    private record ChatDelivery(
            String receiverUsername,
            ChatMessageResponse senderResponse,
            ChatMessageResponse receiverResponse,
            boolean newlyCreated
    ) {
    }
}

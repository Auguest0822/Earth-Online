package com.earthonline.backend.chat;

import com.earthonline.backend.chat.dto.ChatMessageResponse;
import com.earthonline.backend.chat.dto.ChatUserResponse;
import com.earthonline.backend.chat.dto.SendChatMessageRequest;
import com.earthonline.backend.friends.FriendRequestRepository;
import com.earthonline.backend.friends.FriendRequestStatus;
import com.earthonline.backend.storage.StorageService;
import com.earthonline.backend.user.UserEntity;
import com.earthonline.backend.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final StorageService storageService;

    public ChatService(
            ChatMessageRepository chatMessageRepository,
            UserRepository userRepository,
            FriendRequestRepository friendRequestRepository,
            StorageService storageService
    ) {
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
        this.friendRequestRepository = friendRequestRepository;
        this.storageService = storageService;
    }

    /*
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

    @Transactional
    public ChatMessageResponse sendMessage(String currentUsername, SendChatMessageRequest request) {
        UserEntity sender = findUserByUsername(currentUsername);
        UserEntity receiver = findUserByPublicId(request.recipientPublicId());
        requireFriendship(sender, receiver);

        String content = request.content().trim();
        ChatMessageEntity message = chatMessageRepository.save(new ChatMessageEntity(sender, receiver, content));
        return toResponse(message, sender.getId());
    }

    private void requireFriendship(UserEntity currentUser, UserEntity friend) {
        if (currentUser.getId().equals(friend.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不能给自己发送消息");
        }

        boolean friends = friendRequestRepository
                .findRelationshipsBetween(currentUser.getId(), friend.getId())
                .stream()
                .anyMatch(request -> request.getStatus() == FriendRequestStatus.ACCEPTED);
        if (!friends) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只能向已添加的好友发送消息");
        }
    }

    private UserEntity findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录状态无效"));
    }

    private UserEntity findUserByPublicId(String publicId) {
        return userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到该地球 ID 对应的用户"));
    }

    private ChatMessageResponse toResponse(ChatMessageEntity message, Long currentUserId) {
        UserEntity sender = message.getSender();
        return new ChatMessageResponse(
                message.getId(),
                new ChatUserResponse(
                        sender.getPublicId(),
                        sender.getUsername(),
                        storageService.getAvatarPreviewUrl(sender.getUsername(), sender.getAvatar())
                ),
                message.getContent(),
                message.getCreatedAt(),
                sender.getId().equals(currentUserId)
        );
    }
    */

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

    @Transactional
    public ChatMessageResponse sendMessage(String currentUsername, SendChatMessageRequest request) {
        UserEntity sender = findUserByUsername(currentUsername);
        UserEntity receiver = findUserByPublicId(request.recipientPublicId());
        requireFriendship(sender, receiver);
        ChatMessageEntity message = chatMessageRepository.save(
                new ChatMessageEntity(sender, receiver, request.content().trim())
        );
        return toResponse(message, sender.getId());
    }

    private void requireFriendship(UserEntity currentUser, UserEntity friend) {
        if (currentUser.getId().equals(friend.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot message yourself");
        }
        boolean friends = friendRequestRepository.findRelationshipsBetween(currentUser.getId(), friend.getId())
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
        return new ChatMessageResponse(
                message.getId(),
                new ChatUserResponse(
                        sender.getPublicId(),
                        sender.getUsername(),
                        storageService.getAvatarPreviewUrl(sender.getUsername(), sender.getAvatar())
                ),
                message.getContent(),
                message.getCreatedAt(),
                sender.getId().equals(currentUserId)
        );
    }
}

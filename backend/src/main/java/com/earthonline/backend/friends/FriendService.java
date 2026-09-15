package com.earthonline.backend.friends;

import com.earthonline.backend.friends.dto.FriendSearchResponse;
import com.earthonline.backend.friends.dto.FriendRequestResponse;
import com.earthonline.backend.friends.dto.FriendListItemResponse;
import com.earthonline.backend.friends.dto.FriendEventResponse;
import com.earthonline.backend.storage.StorageService;
import com.earthonline.backend.user.UserEntity;
import com.earthonline.backend.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class FriendService {

    private final UserRepository userRepository;
    private final StorageService storageService;
    private final FriendRequestRepository friendRequestRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public FriendService(
            UserRepository userRepository,
            StorageService storageService,
            FriendRequestRepository friendRequestRepository,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.friendRequestRepository = friendRequestRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public FriendSearchResponse searchByPublicId(String currentUsername, String publicId) {
        UserEntity currentUser = findUserByUsername(currentUsername);
        UserEntity user = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "未找到该地球 ID 对应的用户"
                ));

        if (user.getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不能搜索自己");
        }

        return toSearchResponse(user, relationshipStatus(currentUser, user));
    }

    public FriendRequestResponse createRequest(String currentUsername, String publicId) {
        UserEntity sender = findUserByUsername(currentUsername);
        UserEntity receiver = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "未找到该地球 ID 对应的用户"
                ));

        if (sender.getId().equals(receiver.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不能添加自己为好友");
        }

        FriendRequestEntity existingRequest = friendRequestRepository
                .findBySender_IdAndReceiver_Id(sender.getId(), receiver.getId())
                .orElse(null);

        if (existingRequest != null) {
            if (existingRequest.getStatus() == FriendRequestStatus.PENDING) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "已经发送过好友申请");
            }
            if (existingRequest.getStatus() == FriendRequestStatus.ACCEPTED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "你们已经是好友");
            }
            existingRequest.resend();
            FriendRequestEntity resentRequest = friendRequestRepository.save(existingRequest);
            // 重新申请同样要实时通知对方，否则对方只能靠刷新才发现
            pushFriendEvent(receiver, "REQUEST_RECEIVED", sender.getPublicId());
            return toRequestResponse(
                    resentRequest,
                    receiver,
                    FriendRelationStatus.PENDING_SENT
            );
        }

        FriendRequestEntity reverseRequest = friendRequestRepository
                .findBySender_IdAndReceiver_Id(receiver.getId(), sender.getId())
                .orElse(null);

        if (reverseRequest != null) {
            if (reverseRequest.getStatus() == FriendRequestStatus.PENDING) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "对方已经向你发送好友申请，请先处理该申请"
                );
            }
            if (reverseRequest.getStatus() == FriendRequestStatus.ACCEPTED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "你们已经是好友");
            }
        }

        FriendRequestEntity request = friendRequestRepository.save(new FriendRequestEntity(sender, receiver));
        // 通过 WebSocket 把“收到新的好友申请”实时推给对方：
        // 对方不需要刷新页面或切到朋友页，就能在任意页面看到提醒（侧边栏红点 + 提示音）。
        pushFriendEvent(receiver, "REQUEST_RECEIVED", sender.getPublicId());
        return toRequestResponse(request, receiver, FriendRelationStatus.PENDING_SENT);
    }

    public List<FriendRequestResponse> getReceivedRequests(String currentUsername) {
        UserEntity currentUser = findUserByUsername(currentUsername);
        return friendRequestRepository
                .findByReceiver_IdAndStatusOrderByCreatedAtDesc(currentUser.getId(), FriendRequestStatus.PENDING)
                .stream()
                .map(request -> toRequestResponse(
                        request,
                        request.getSender(),
                        FriendRelationStatus.PENDING_RECEIVED
                ))
                .toList();
    }

    public FriendRequestResponse acceptRequest(String currentUsername, Long requestId) {
        FriendRequestEntity request = findReceivedPendingRequest(currentUsername, requestId);
        request.accept();
        FriendRequestEntity savedRequest = friendRequestRepository.save(request);
        pushFriendEvent(
                savedRequest.getSender(),
                "ACCEPTED",
                savedRequest.getReceiver().getPublicId()
        );
        return toRequestResponse(savedRequest, savedRequest.getSender(), FriendRelationStatus.FRIENDS);
    }

    public FriendRequestResponse rejectRequest(String currentUsername, Long requestId) {
        FriendRequestEntity request = findReceivedPendingRequest(currentUsername, requestId);
        request.reject();
        FriendRequestEntity savedRequest = friendRequestRepository.save(request);
        pushFriendEvent(
                savedRequest.getSender(),
                "REJECTED",
                savedRequest.getReceiver().getPublicId()
        );
        return toRequestResponse(savedRequest, savedRequest.getSender(), FriendRelationStatus.NONE);
    }

    public List<FriendListItemResponse> getFriends(String currentUsername) {
        UserEntity currentUser = findUserByUsername(currentUsername);
        return friendRequestRepository
                .findBySender_IdAndStatusOrReceiver_IdAndStatusOrderByUpdatedAtDesc(
                        currentUser.getId(),
                        FriendRequestStatus.ACCEPTED,
                        currentUser.getId(),
                        FriendRequestStatus.ACCEPTED
                )
                .stream()
                .map(request -> {
                    UserEntity friend = request.getSender().getId().equals(currentUser.getId())
                            ? request.getReceiver()
                            : request.getSender();
                    return new FriendListItemResponse(
                            request.getId(),
                            friend.getPublicId(),
                            friend.getUsername(),
                            storageService.getAvatarPreviewUrl(friend.getUsername(), friend.getAvatar()),
                            friend.getSlogan(),
                            request.getUpdatedAt()
                    );
                })
                .toList();
    }

    @Transactional
    public void removeFriend(String currentUsername, String friendPublicId) {
        UserEntity currentUser = findUserByUsername(currentUsername);
        UserEntity friend = userRepository.findByPublicId(friendPublicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "好友不存在"));

        List<FriendRequestEntity> relationships = friendRequestRepository
                .findRelationshipsBetween(currentUser.getId(), friend.getId());
        List<FriendRequestEntity> accepted = relationships.stream()
                .filter(request -> request.getStatus() == FriendRequestStatus.ACCEPTED)
                .toList();
        if (accepted.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "好友关系不存在");
        }
        accepted.forEach(FriendRequestEntity::reject);
        friendRequestRepository.saveAll(accepted);
        pushFriendEvent(friend, "REMOVED", currentUser.getPublicId());
    }

    private void pushFriendEvent(UserEntity user, String type, String friendPublicId) {
        messagingTemplate.convertAndSendToUser(
                user.getUsername(),
                "/queue/friends/events",
                new FriendEventResponse(type, friendPublicId)
        );
    }

    private UserEntity findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录状态无效"));
    }

    private FriendRequestEntity findReceivedPendingRequest(String currentUsername, Long requestId) {
        UserEntity currentUser = findUserByUsername(currentUsername);
        FriendRequestEntity request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "好友申请不存在"));

        if (!request.getReceiver().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权处理此好友申请");
        }
        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该好友申请已经处理过");
        }
        return request;
    }

    private FriendRequestResponse toRequestResponse(
            FriendRequestEntity request,
            UserEntity otherUser,
            FriendRelationStatus relationStatus
    ) {
        return new FriendRequestResponse(
                request.getId(),
                request.getStatus(),
                new FriendSearchResponse(
                        otherUser.getPublicId(),
                        otherUser.getUsername(),
                        storageService.getAvatarPreviewUrl(otherUser.getUsername(), otherUser.getAvatar()),
                        otherUser.getSlogan(),
                        relationStatus
                ),
                request.getCreatedAt()
        );
    }

    private FriendSearchResponse toSearchResponse(UserEntity user, FriendRelationStatus relationStatus) {
        return new FriendSearchResponse(
                user.getPublicId(),
                user.getUsername(),
                storageService.getAvatarPreviewUrl(user.getUsername(), user.getAvatar()),
                user.getSlogan(),
                relationStatus
        );
    }

    private FriendRelationStatus relationshipStatus(UserEntity currentUser, UserEntity otherUser) {
        return friendRequestRepository.findRelationshipsBetween(currentUser.getId(), otherUser.getId())
                .stream()
                .findFirst()
                .map(request -> {
                    if (request.getStatus() == FriendRequestStatus.ACCEPTED) {
                        return FriendRelationStatus.FRIENDS;
                    }
                    if (request.getStatus() == FriendRequestStatus.PENDING) {
                        return request.getSender().getId().equals(currentUser.getId())
                                ? FriendRelationStatus.PENDING_SENT
                                : FriendRelationStatus.PENDING_RECEIVED;
                    }
                    return FriendRelationStatus.NONE;
                })
                .orElse(FriendRelationStatus.NONE);
    }
}

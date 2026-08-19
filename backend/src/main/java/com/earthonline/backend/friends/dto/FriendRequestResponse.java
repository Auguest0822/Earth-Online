package com.earthonline.backend.friends.dto;

import com.earthonline.backend.friends.FriendRequestStatus;

import java.time.LocalDateTime;

public record FriendRequestResponse(
        Long id,
        FriendRequestStatus status,
        FriendSearchResponse user,
        LocalDateTime createdAt
) {
}

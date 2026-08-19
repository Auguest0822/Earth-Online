package com.earthonline.backend.friends.dto;

public record FriendEventResponse(
        String type,
        String friendPublicId
) {
}

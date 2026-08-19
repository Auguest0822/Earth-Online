package com.earthonline.backend.friends.dto;

import java.time.LocalDateTime;

/** A confirmed friend shown in the current user's friend list. */
public record FriendListItemResponse(
        Long friendshipId,
        String publicId,
        String username,
        String avatar,
        String slogan,
        LocalDateTime friendsSince
) {
}

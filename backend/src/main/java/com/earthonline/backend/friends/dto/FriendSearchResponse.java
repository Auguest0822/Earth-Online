package com.earthonline.backend.friends.dto;

import com.earthonline.backend.friends.FriendRelationStatus;

/** Public profile fields that are safe to show to another user. */
public record FriendSearchResponse(
        String publicId,
        String username,
        String avatar,
        String slogan,
        FriendRelationStatus relationStatus
) {
}

package com.earthonline.backend.friends;

/** Relationship between the signed-in user and a searched user. */
public enum FriendRelationStatus {
    NONE,
    PENDING_SENT,
    PENDING_RECEIVED,
    FRIENDS
}

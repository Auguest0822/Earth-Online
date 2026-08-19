package com.earthonline.backend.friends;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendRequestRepository extends JpaRepository<FriendRequestEntity, Long> {

    Optional<FriendRequestEntity> findBySender_IdAndReceiver_Id(Long senderId, Long receiverId);

    @Query("""
            select request from FriendRequestEntity request
            where (request.sender.id = :firstUserId and request.receiver.id = :secondUserId)
               or (request.sender.id = :secondUserId and request.receiver.id = :firstUserId)
            order by request.updatedAt desc
            """)
    List<FriendRequestEntity> findRelationshipsBetween(
            @Param("firstUserId") Long firstUserId,
            @Param("secondUserId") Long secondUserId
    );

    List<FriendRequestEntity> findByReceiver_IdAndStatusOrderByCreatedAtDesc(
            Long receiverId,
            FriendRequestStatus status
    );

    List<FriendRequestEntity> findBySender_IdAndStatusOrReceiver_IdAndStatusOrderByUpdatedAtDesc(
            Long senderId,
            FriendRequestStatus senderStatus,
            Long receiverId,
            FriendRequestStatus receiverStatus
    );
}

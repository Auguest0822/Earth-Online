package com.earthonline.backend.friends;

import com.earthonline.backend.friends.dto.FriendSearchResponse;
import com.earthonline.backend.friends.dto.CreateFriendRequest;
import com.earthonline.backend.friends.dto.FriendRequestResponse;
import com.earthonline.backend.friends.dto.FriendListItemResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/friends")
public class FriendController {

    private final FriendService friendService;

    public FriendController(FriendService friendService) {
        this.friendService = friendService;
    }

    @GetMapping("/search")
    public FriendSearchResponse search(
            @RequestParam
            @Pattern(regexp = "\\d{10}", message = "地球 ID 必须是 10 位数字")
            String publicId,
            Authentication authentication
    ) {
        return friendService.searchByPublicId(authentication.getName(), publicId);
    }

    @PostMapping("/requests")
    public FriendRequestResponse createRequest(
            @Valid @RequestBody CreateFriendRequest request,
            Authentication authentication
    ) {
        return friendService.createRequest(authentication.getName(), request.publicId());
    }

    @GetMapping("/requests/received")
    public java.util.List<FriendRequestResponse> getReceivedRequests(Authentication authentication) {
        return friendService.getReceivedRequests(authentication.getName());
    }

    @PostMapping("/requests/{requestId}/accept")
    public FriendRequestResponse acceptRequest(
            @PathVariable Long requestId,
            Authentication authentication
    ) {
        return friendService.acceptRequest(authentication.getName(), requestId);
    }

    @PostMapping("/requests/{requestId}/reject")
    public FriendRequestResponse rejectRequest(
            @PathVariable Long requestId,
            Authentication authentication
    ) {
        return friendService.rejectRequest(authentication.getName(), requestId);
    }

    @GetMapping
    public java.util.List<FriendListItemResponse> getFriends(Authentication authentication) {
        return friendService.getFriends(authentication.getName());
    }
}

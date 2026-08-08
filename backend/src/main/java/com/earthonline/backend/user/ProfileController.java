package com.earthonline.backend.user;

import com.earthonline.backend.user.dto.UpdateProfileRequest;
import com.earthonline.backend.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public UserResponse getProfile(
            Authentication authentication
    ) {
        return profileService.getProfile(
                authentication.getName()
        );
    }

    @PatchMapping
    public UserResponse updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication
    ) {
        return profileService.updateProfile(
                authentication.getName(),
                request
        );
    }
}
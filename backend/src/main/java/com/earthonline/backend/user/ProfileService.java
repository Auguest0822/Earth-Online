package com.earthonline.backend.user;

import com.earthonline.backend.user.dto.UpdateProfileRequest;
import com.earthonline.backend.user.dto.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProfileService {

    private final UserRepository userRepository;

    public ProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse getProfile(String username) {
        UserEntity user = findUser(username);
        return toResponse(user);
    }

    public UserResponse updateProfile(
            String username,
            UpdateProfileRequest request
    ) {
        UserEntity user = findUser(username);

        if (request.email() != null
                && !request.email().isBlank()) {
            user.setEmail(request.email());
        }

        if (request.avatar() != null) {
            user.setAvatar(request.avatar());
        }

        if (request.slogan() != null) {
            user.setSlogan(request.slogan());
        }

        if (request.birthDate() != null) {
            user.setBirthDate(request.birthDate());
        }

        return toResponse(userRepository.save(user));
    }

    private UserEntity findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "用户不存在"
                ));
    }

    private UserResponse toResponse(UserEntity user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getAvatar(),
                user.getSlogan(),
                user.getBirthDate()
        );
    }
}
package com.earthonline.backend.user;

import com.earthonline.backend.user.dto.RegisterRequest;
import com.earthonline.backend.user.dto.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

import com.earthonline.backend.auth.JwtService;
import com.earthonline.backend.storage.StorageService;
import com.earthonline.backend.user.dto.LoginResponse;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final StorageService storageService;
    private final PublicIdService publicIdService;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            StorageService storageService,
            PublicIdService publicIdService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.storageService = storageService;
        this.publicIdService = publicIdService;
    }

    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "用户名已存在"
            );
        }

        String passwordHash =
                passwordEncoder.encode(request.password());

        UserEntity user = new UserEntity(
                request.username(),
                passwordHash,
                request.email(),
                LocalDateTime.now()
        );
        user.setPublicId(publicIdService.generateUniquePublicId());

        UserEntity saved = userRepository.save(user);

        return toResponse(saved);
    }

    public LoginResponse login(
            String username,
            String password
    ) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "用户名或密码错误"
                ));

        boolean passwordMatches =
                passwordEncoder.matches(
                        password,
                        user.getPasswordHash()
                );

        if (!passwordMatches) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "用户名或密码错误"
            );
        }

        UserResponse userResponse = toResponse(user);

        String token = jwtService.generateToken(
                user.getId(),
                user.getUsername()
        );

        return new LoginResponse(token, userResponse);
    }

    public UserResponse getCurrentUser(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "用户不存在"
                ));

        return toResponse(user);
    }

    private UserResponse toResponse(UserEntity user) {
        return new UserResponse(
                user.getId(),
                user.getPublicId(),
                user.getUsername(),
                user.getEmail(),
                storageService.getAvatarPreviewUrl(user.getUsername(), user.getAvatar()),
                user.getSlogan(),
                user.getBirthDate()
        );
    }

}

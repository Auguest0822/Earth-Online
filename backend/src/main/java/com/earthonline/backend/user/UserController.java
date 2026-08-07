package com.earthonline.backend.user;

import com.earthonline.backend.user.dto.RegisterRequest;
import com.earthonline.backend.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import com.earthonline.backend.user.dto.LoginRequest;

@RestController
@RequestMapping("/api/auth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public UserResponse register(
            @Valid @RequestBody RegisterRequest request
    ) {
        return userService.register(request);
    }

    @PostMapping("/login")
    public UserResponse login(
            @Valid @RequestBody LoginRequest request
    ) {
        return userService.login(
                request.username(),
                request.password()
        );
    }

}
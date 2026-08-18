package com.earthonline.backend.user;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class PublicIdService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private final UserRepository userRepository;

    public PublicIdService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Generates a non-zero-leading, unique ten-digit public ID. */
    public String generateUniquePublicId() {
        String publicId;
        do {
            StringBuilder builder = new StringBuilder(10);
            builder.append(RANDOM.nextInt(9) + 1);
            for (int index = 1; index < 10; index++) {
                builder.append(RANDOM.nextInt(10));
            }
            publicId = builder.toString();
        } while (userRepository.existsByPublicId(publicId));
        return publicId;
    }
}

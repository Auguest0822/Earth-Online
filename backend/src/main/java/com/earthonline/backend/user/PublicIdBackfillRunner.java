package com.earthonline.backend.user;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Gives existing accounts a public ID exactly once after the column is added. */
@Component
public class PublicIdBackfillRunner implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PublicIdService publicIdService;

    public PublicIdBackfillRunner(UserRepository userRepository, PublicIdService publicIdService) {
        this.userRepository = userRepository;
        this.publicIdService = publicIdService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        userRepository.findAll().forEach(user -> {
            if (user.getPublicId() == null || !user.getPublicId().matches("\\d{10}")) {
                user.setPublicId(publicIdService.generateUniquePublicId());
            }
        });
    }
}

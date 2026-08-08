package com.earthonline.backend.futureletters;

import com.earthonline.backend.futureletters.dto.FutureLetterRequest;
import com.earthonline.backend.futureletters.dto.FutureLetterResponse;
import com.earthonline.backend.user.UserEntity;
import com.earthonline.backend.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FutureLetterService {

    private final FutureLetterRepository futureLetterRepository;
    private final UserRepository userRepository;

    public FutureLetterService(
            FutureLetterRepository futureLetterRepository,
            UserRepository userRepository
    ) {
        this.futureLetterRepository = futureLetterRepository;
        this.userRepository = userRepository;
    }

    public List<FutureLetterResponse> list(String username) {
        Long userId = requireUserId(username);

        return futureLetterRepository
                .findAllByUserIdOrderByOpenAtAsc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public FutureLetterResponse create(
            String username,
            FutureLetterRequest request
    ) {
        Long userId = requireUserId(username);

        FutureLetterEntity entity = new FutureLetterEntity(
                userId,
                request.title(),
                request.content(),
                request.mood(),
                request.openAt().atStartOfDay(),
                defaultValue(request.status(), "sealed"),
                defaultValue(request.visibility(), "private"),
                request.allowEditBeforeOpen() == null
                        || request.allowEditBeforeOpen(),
                LocalDateTime.now()
        );

        return toResponse(futureLetterRepository.save(entity));
    }

    public FutureLetterResponse findById(
            String username,
            Long id
    ) {
        Long userId = requireUserId(username);

        FutureLetterEntity entity = futureLetterRepository
                .findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "未来信件不存在"
                ));

        return toResponse(entity);
    }

    public FutureLetterResponse update(
            String username,
            Long id,
            FutureLetterRequest request
    ) {
        Long userId = requireUserId(username);

        FutureLetterEntity entity = futureLetterRepository
                .findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "未来信件不存在"
                ));

        entity.setTitle(request.title());
        entity.setContent(request.content());
        entity.setMood(request.mood());
        entity.setOpenAt(request.openAt().atStartOfDay());

        if (request.status() != null) {
            entity.setStatus(request.status());
        }

        if (request.visibility() != null) {
            entity.setVisibility(request.visibility());
        }

        if (request.allowEditBeforeOpen() != null) {
            entity.setAllowEditBeforeOpen(
                    request.allowEditBeforeOpen()
            );
        }

        return toResponse(futureLetterRepository.save(entity));
    }

    public void delete(String username, Long id) {
        Long userId = requireUserId(username);

        if (!futureLetterRepository.existsByIdAndUserId(id, userId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "未来信件不存在"
            );
        }

        futureLetterRepository.deleteByIdAndUserId(id, userId);
    }

    private Long requireUserId(String username) {
        UserEntity user = userRepository
                .findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "用户不存在"
                ));

        return user.getId();
    }

    private FutureLetterResponse toResponse(
            FutureLetterEntity entity
    ) {
        return new FutureLetterResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getContent(),
                entity.getMood(),
                entity.getOpenAt(),
                entity.getStatus(),
                entity.getVisibility(),
                entity.getAllowEditBeforeOpen(),
                entity.getCreatedAt()
        );
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.isBlank()
                ? fallback
                : value;
    }
}
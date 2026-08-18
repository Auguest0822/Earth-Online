package com.earthonline.backend.records;

import com.earthonline.backend.records.dto.CreateRecordRequest;
import com.earthonline.backend.records.dto.RecordResponse;
import com.earthonline.backend.user.UserEntity;
import com.earthonline.backend.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

@Service
public class RecordService {

    private final RecordRepository recordRepository;
    private final UserRepository userRepository;

    public RecordService(
            RecordRepository recordRepository,
            UserRepository userRepository
    ) {
        this.recordRepository = recordRepository;
        this.userRepository = userRepository;
    }

    public List<RecordResponse> list(String username) {
        Long userId = requireUserId(username);

        return recordRepository.findAllByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public RecordResponse create(
            String username,
            CreateRecordRequest request
    ) {
        Long userId = requireUserId(username);

        RecordEntity entity = new RecordEntity(
                normalizeTitle(request.title(), request.content()),
                request.content(),
                LocalDateTime.now(),
                userId,
                request.date() == null ? LocalDate.now() : request.date(),
                request.mood(),
                request.keywords(),
                request.photos()
        );

        return toResponse(recordRepository.save(entity));
    }

    public RecordResponse findById(String username, Long id) {
        Long userId = requireUserId(username);

        RecordEntity entity = recordRepository
                .findByIdAndUserId(id, userId)
                .orElseThrow(() -> recordNotFound());

        return toResponse(entity);
    }

    public RecordResponse update(
            String username,
            Long id,
            CreateRecordRequest request
    ) {
        Long userId = requireUserId(username);

        RecordEntity entity = recordRepository
                .findByIdAndUserId(id, userId)
                .orElseThrow(() -> recordNotFound());

        entity.setTitle(normalizeTitle(request.title(), request.content()));
        entity.setContent(request.content());
        entity.setRecordDate(request.date() == null ? entity.getRecordDate() : request.date());
        entity.setMood(request.mood());
        entity.setKeywords(request.keywords());
        entity.setPhotos(request.photos());

        return toResponse(recordRepository.save(entity));
    }

    public void delete(String username, Long id) {
        Long userId = requireUserId(username);

        if (!recordRepository.existsByIdAndUserId(id, userId)) {
            throw recordNotFound();
        }

        recordRepository.deleteByIdAndUserId(id, userId);
    }

    private Long requireUserId(String username) {
        return userRepository.findByUsername(username)
                .map(UserEntity::getId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "用户不存在"
                ));
    }

    private ResponseStatusException recordNotFound() {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "记录不存在"
        );
    }

    private RecordResponse toResponse(RecordEntity entity) {
        return new RecordResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getContent(),
                entity.getRecordDate(),
                entity.getMood(),
                List.copyOf(entity.getKeywords()),
                List.copyOf(entity.getPhotos()),
                entity.getCreatedAt()
        );
    }

    private String normalizeTitle(String title, String content) {
        if (title != null && !title.isBlank()) return title.trim();
        String normalizedContent = content.trim().replaceAll("\\s+", " ");
        return normalizedContent.length() <= 30
                ? normalizedContent
                : normalizedContent.substring(0, 30) + "…";
    }
}

package com.earthonline.backend.records;

import com.earthonline.backend.records.dto.CreateRecordRequest;
import com.earthonline.backend.records.dto.RecordResponse;
import com.earthonline.backend.records.dto.RecordPageResponse;
import com.earthonline.backend.storage.StorageService;
import com.earthonline.backend.user.UserEntity;
import com.earthonline.backend.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.PageRequest;

@Service
public class RecordService {

    private final RecordRepository recordRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;

    public RecordService(
            RecordRepository recordRepository,
            UserRepository userRepository,
            StorageService storageService
    ) {
        this.recordRepository = recordRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
    }

    public List<RecordResponse> list(String username) {
        Long userId = requireUserId(username);

        return recordRepository.findAllByUserId(userId)
                .stream()
                .map(entity -> toResponse(entity, username))
                .toList();
    }

    public RecordPageResponse listPage(
            String username,
            int page,
            int pageSize,
            Integer year,
            String tag,
            String eventType
    ) {
        Long userId = requireUserId(username);
        int safePage = Math.max(1, page);
        int safePageSize = Math.min(100, Math.max(1, pageSize));
        String normalizedTag = normalizeOptionalFilter(tag);
        String normalizedEventType = normalizeOptionalFilter(eventType);
        LocalDate startDate = year == null ? null : LocalDate.of(year, 1, 1);
        LocalDate endDate = year == null ? null : LocalDate.of(year, 12, 31);

        var result = recordRepository.findPageByFilters(
                userId,
                startDate,
                endDate,
                normalizedEventType,
                normalizedTag,
                PageRequest.of(safePage - 1, safePageSize)
        );

        return new RecordPageResponse(
                result.getContent().stream().map(entity -> toResponse(entity, username)).toList(),
                result.getTotalElements(),
                safePage,
                safePageSize,
                result.getTotalPages(),
                recordRepository.findDistinctYearsByUserId(userId),
                recordRepository.findDistinctTagsByUserId(userId),
                recordRepository.findDistinctEventTypesByUserId(userId)
        );
    }

    public List<RecordResponse> search(String username, String query) {
        Long userId = requireUserId(username);
        String normalizedQuery = query == null ? "" : query.trim();
        if (normalizedQuery.isEmpty()) return List.of();
        return recordRepository.searchByUser(userId, normalizedQuery, PageRequest.of(0, 20))
                .stream()
                .map(entity -> toResponse(entity, username))
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
                normalizeEventType(request.eventType()),
                request.mood(),
                request.keywords(),
                validatePhotoObjectKeys(username, request.photos())
        );

        return toResponse(recordRepository.save(entity), username);
    }

    public RecordResponse findById(String username, Long id) {
        Long userId = requireUserId(username);

        RecordEntity entity = recordRepository
                .findByIdAndUserId(id, userId)
                .orElseThrow(() -> recordNotFound());

        return toResponse(entity, username);
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
        entity.setEventType(normalizeEventType(request.eventType()));
        entity.setMood(request.mood());
        entity.setKeywords(request.keywords());
        entity.setPhotos(validatePhotoObjectKeys(username, request.photos()));

        return toResponse(recordRepository.save(entity), username);
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

    private RecordResponse toResponse(RecordEntity entity, String username) {
        return new RecordResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getContent(),
                entity.getRecordDate(),
                entity.getEventType(),
                entity.getMood(),
                List.copyOf(entity.getKeywords()),
                entity.getPhotos().stream()
                        .map(photo -> toPreviewUrl(username, photo))
                        .filter(java.util.Objects::nonNull)
                        .toList(),
                List.copyOf(entity.getPhotos()),
                entity.getCreatedAt()
        );
    }

    private List<String> validatePhotoObjectKeys(String username, List<String> photos) {
        String expectedPrefix = "records/" + username + "/";

        for (String photo : photos) {
            if (photo == null || !photo.startsWith(expectedPrefix)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "图片必须先通过上传接口上传"
                );
            }
        }

        return List.copyOf(photos);
    }

    private String toPreviewUrl(String username, String photo) {
        // 兼容早期已保存的 http 图片地址；新记录统一保存 MinIO 对象名。
        if (photo.startsWith("http://") || photo.startsWith("https://")) {
            return photo;
        }

        String expectedPrefix = "records/" + username + "/";
        if (!photo.startsWith(expectedPrefix)) {
            return null;
        }

        return storageService.getPreviewUrlOrNull(photo);
    }

    private String normalizeTitle(String title, String content) {
        if (title != null && !title.isBlank()) return title.trim();
        String normalizedContent = content.trim().replaceAll("\\s+", " ");
        return normalizedContent.length() <= 30
                ? normalizedContent
                : normalizedContent.substring(0, 30) + "…";
    }

    private String normalizeEventType(String eventType) {
        return eventType == null || eventType.isBlank() ? "生活日常" : eventType.trim();
    }

    private String normalizeOptionalFilter(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

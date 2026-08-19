package com.earthonline.backend.ai;

import com.earthonline.backend.ai.dto.KeywordInsightResponse;
import com.earthonline.backend.ai.dto.YearlySummaryResponse;
import com.earthonline.backend.records.RecordEntity;
import com.earthonline.backend.records.RecordRepository;
import com.earthonline.backend.user.UserEntity;
import com.earthonline.backend.user.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
public class YearlySummaryService {
    private static final String PROMPT_VERSION = "v1";

    private final UserRepository userRepository;
    private final RecordRepository recordRepository;
    private final YearlySummaryRepository summaryRepository;
    private final YearlySummaryAiClient aiClient;
    private final ObjectMapper objectMapper;

    public YearlySummaryService(UserRepository userRepository, RecordRepository recordRepository,
                                YearlySummaryRepository summaryRepository, YearlySummaryAiClient aiClient,
                                ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.recordRepository = recordRepository;
        this.summaryRepository = summaryRepository;
        this.aiClient = aiClient;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Optional<YearlySummaryResponse> getCached(String username, int year) {
        UserEntity user = requireUser(username);
        List<RecordEntity> records = recordsForYear(user.getId(), year);
        String sourceHash = sourceHash(records);
        return summaryRepository.findByUserIdAndYear(user.getId(), year)
                .map(entity -> toResponse(entity, !entity.getSourceHash().equals(sourceHash)));
    }

    public YearlySummaryResponse generate(String username, int year, boolean regenerate) {
        UserEntity user = requireUser(username);
        List<RecordEntity> records = recordsForYear(user.getId(), year);
        if (records.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, year + " 年暂无记录，无法生成年度总结");
        }
        String sourceHash = sourceHash(records);
        Optional<YearlySummaryEntity> cached = summaryRepository.findByUserIdAndYear(user.getId(), year);
        if (!regenerate && cached.isPresent() && cached.get().getSourceHash().equals(sourceHash)) {
            return toResponse(cached.get(), false);
        }

        DeepSeekYearlySummary result = aiClient.generate(year, records);
        YearlySummaryEntity entity = cached.orElseGet(() -> new YearlySummaryEntity(user.getId(), year));
        try {
            entity.update(result.title(), result.summary(), result.moodOverview(),
                    objectMapper.writeValueAsString(result.keywordInsights()),
                    objectMapper.writeValueAsString(result.highlights()), result.letterToFuture(),
                    records.size(), sourceHash, aiClient.model(), PROMPT_VERSION);
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "年度总结保存失败", exception);
        }
        return toResponse(summaryRepository.save(entity), false);
    }

    private List<RecordEntity> recordsForYear(Long userId, int year) {
        return recordRepository.findAllByUserIdAndRecordDateBetweenOrderByRecordDateAsc(
                userId, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
    }

    private UserEntity requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录状态无效"));
    }

    private String sourceHash(List<RecordEntity> records) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (RecordEntity record : records) {
                String stable = "%s\u001f%s\u001f%s\u001f%s\u001f%s\u001f%s\n".formatted(
                        record.getId(), record.getRecordDate(), record.getTitle(), record.getContent(),
                        record.getMood(), String.join("\u001e", record.getKeywords()));
                digest.update(stable.getBytes(StandardCharsets.UTF_8));
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot hash yearly records", exception);
        }
    }

    private YearlySummaryResponse toResponse(YearlySummaryEntity entity, boolean stale) {
        try {
            List<KeywordInsightResponse> keywords = objectMapper.readValue(entity.getKeywordInsightsJson(),
                    new TypeReference<List<KeywordInsightResponse>>() {});
            List<String> highlights = objectMapper.readValue(entity.getHighlightsJson(),
                    new TypeReference<List<String>>() {});
            return new YearlySummaryResponse(entity.getId(), entity.getYear(), entity.getTitle(),
                    entity.getSummary(), entity.getMoodOverview(), keywords, highlights,
                    entity.getLetterToFuture(), entity.getRecordCount(), entity.getModel(), stale,
                    entity.getUpdatedAt());
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "年度总结读取失败", exception);
        }
    }
}

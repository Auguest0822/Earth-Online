package com.earthonline.backend.ai;

import com.earthonline.backend.ai.dto.MemorySourceResponse;
import com.earthonline.backend.ai.dto.YearlyMemoryResponse;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
public class YearlyMemoryService {
    private static final int MAX_SOURCES = 5;
    private static final int EXCERPT_LENGTH = 120;
    /** 提示词版本：以后改了提示词，可以用它判断旧缓存要不要重算。 */
    private static final String PROMPT_VERSION = "v1";
    private static final Set<String> QUESTION_FILLERS = Set.of(
            "这一年", "今年", "哪些", "什么", "怎么", "如何", "有没有", "请", "帮我", "一下", "事情", "记录");

    private final UserRepository userRepository;
    private final RecordRepository recordRepository;
    private final YearlyMemoryAnswerRepository answerRepository;
    private final YearlyMemoryAiClient aiClient;
    private final ObjectMapper objectMapper;

    public YearlyMemoryService(UserRepository userRepository, RecordRepository recordRepository,
                               YearlyMemoryAnswerRepository answerRepository, YearlyMemoryAiClient aiClient,
                               ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.recordRepository = recordRepository;
        this.answerRepository = answerRepository;
        this.aiClient = aiClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 读取上一次的问答缓存。没有缓存时返回空（前端会显示成“还没有问过”）。
     * 与年度总结的 GET 接口行为保持一致。
     */
    @Transactional(readOnly = true)
    public Optional<YearlyMemoryResponse> getSaved(String username, int year) {
        UserEntity user = requireUser(username);
        String sourceHash = YearlyRecordFingerprint.of(recordsForYear(user.getId(), year));
        return answerRepository.findByUserIdAndYear(user.getId(), year)
                .map(entity -> toResponse(entity, !entity.getSourceHash().equals(sourceHash)));
    }

    /** 提问并回答，同时把这次问答覆盖保存为该用户该年的缓存。 */
    @Transactional
    public YearlyMemoryResponse query(String username, int year, String rawQuestion) {
        UserEntity user = requireUser(username);
        List<RecordEntity> yearlyRecords = recordsForYear(user.getId(), year);
        if (yearlyRecords.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, year + " 年暂无记录，无法进行回忆问答");
        }

        String question = rawQuestion.trim();
        List<ScoredRecord> ranked = yearlyRecords.stream()
                .map(record -> new ScoredRecord(record, relevance(question, record)))
                .sorted(Comparator.comparingDouble(ScoredRecord::score).reversed()
                        .thenComparing(item -> item.record().getRecordDate(), Comparator.reverseOrder()))
                .toList();

        List<RecordEntity> sources = new ArrayList<>(ranked.stream()
                .filter(item -> item.score() > 0)
                .limit(MAX_SOURCES)
                .map(ScoredRecord::record).toList());

        // For broad questions such as “总结这一年的成长”, lexical matches may be sparse.
        // Fill the remaining context with the latest records, while keeping the user/year boundary intact.
        if (sources.size() < Math.min(3, yearlyRecords.size())) {
            ranked.stream().map(ScoredRecord::record).filter(record -> !sources.contains(record))
                    .limit(Math.min(3, yearlyRecords.size()) - sources.size()).forEach(sources::add);
        }

        String answer = aiClient.answer(year, question, sources);
        List<MemorySourceResponse> sourceResponses = sources.stream().map(record -> {
            double rawScore = relevance(question, record);
            double normalized = Math.min(1, rawScore / 30.0);
            return new MemorySourceResponse(record.getId(), record.getTitle(), record.getRecordDate(),
                    excerpt(record.getContent()), Math.round(normalized * 100.0) / 100.0);
        }).toList();

        YearlyMemoryAnswerEntity entity = answerRepository.findByUserIdAndYear(user.getId(), year)
                .orElseGet(() -> new YearlyMemoryAnswerEntity(user.getId(), year));
        try {
            entity.update(question, answer, objectMapper.writeValueAsString(toSavedSources(sourceResponses)),
                    YearlyRecordFingerprint.of(yearlyRecords), aiClient.model(), PROMPT_VERSION);
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "回忆问答保存失败", exception);
        }
        YearlyMemoryAnswerEntity saved = answerRepository.save(entity);
        return new YearlyMemoryResponse(question, answer, year, sourceResponses, false, saved.getUpdatedAt());
    }

    /**
     * 存进数据库的“回答依据”。
     *
     * 为什么要单独定义一个 record，而不是直接把 MemorySourceResponse 序列化？
     * 因为这里注入的是 Jackson 2（AI 相关代码统一用它），而项目里没有引入 Jackson 2 的
     * jsr310 模块，它不认识 java.time.LocalDate。于是把日期先转成 ISO 字符串再存，
     * 读出来时再解析回 LocalDate，就能绕开模块缺失的问题，也保证存进库的 JSON 是可读的。
     */
    private record SavedSource(Long recordId, String title, String date, String excerpt, double score) {
    }

    private List<SavedSource> toSavedSources(List<MemorySourceResponse> sources) {
        return sources.stream()
                .map(item -> new SavedSource(item.recordId(), item.title(),
                        item.date() == null ? null : item.date().toString(), item.excerpt(), item.score()))
                .toList();
    }

    private List<MemorySourceResponse> fromSavedSources(String json) throws Exception {
        List<SavedSource> saved = objectMapper.readValue(json, new TypeReference<List<SavedSource>>() {});
        return saved.stream()
                .map(item -> new MemorySourceResponse(item.recordId(), item.title(),
                        item.date() == null ? null : LocalDate.parse(item.date()), item.excerpt(), item.score()))
                .toList();
    }

    private YearlyMemoryResponse toResponse(YearlyMemoryAnswerEntity entity, boolean stale) {
        try {
            return new YearlyMemoryResponse(entity.getQuestion(), entity.getAnswer(), entity.getYear(),
                    fromSavedSources(entity.getSourcesJson()), stale, entity.getUpdatedAt());
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "回忆问答读取失败", exception);
        }
    }

    private List<RecordEntity> recordsForYear(Long userId, int year) {
        return recordRepository.findAllByUserIdAndRecordDateBetweenOrderByRecordDateAsc(
                userId, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
    }

    private UserEntity requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录状态无效"));
    }

    private double relevance(String question, RecordEntity record) {
        String normalizedQuestion = normalize(question);
        String title = normalize(record.getTitle());
        String content = normalize(record.getContent());
        String eventType = normalize(record.getEventType());
        String mood = normalize(record.getMood());
        String keywords = normalize(String.join(" ", record.getKeywords()));
        double score = 0;

        if (!title.isBlank() && (normalizedQuestion.contains(title) || title.contains(normalizedQuestion))) score += 14;
        if (!eventType.isBlank() && normalizedQuestion.contains(eventType)) score += 7;
        if (!mood.isBlank() && normalizedQuestion.contains(mood)) score += 7;
        for (String keyword : record.getKeywords()) {
            String value = normalize(keyword);
            if (!value.isBlank() && normalizedQuestion.contains(value)) score += 9;
        }
        for (String token : queryTokens(normalizedQuestion)) {
            if (title.contains(token)) score += 4;
            if (keywords.contains(token)) score += 5;
            if (eventType.contains(token) || mood.contains(token)) score += 3;
            if (content.contains(token)) score += 1;
        }
        return score;
    }

    private Set<String> queryTokens(String question) {
        String cleaned = question;
        for (String filler : QUESTION_FILLERS) cleaned = cleaned.replace(filler, "");
        Set<String> result = new LinkedHashSet<>();
        for (String part : cleaned.split("[^\\p{IsHan}\\p{L}\\p{N}]+")) {
            if (part.length() == 1) result.add(part);
            for (int i = 0; i < part.length() - 1; i++) result.add(part.substring(i, i + 2));
        }
        return result;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("\\s+", "").trim();
    }

    private String excerpt(String content) {
        String value = content == null ? "" : content.replaceAll("\\s+", " ").trim();
        return value.length() <= EXCERPT_LENGTH ? value : value.substring(0, EXCERPT_LENGTH) + "…";
    }

    private record ScoredRecord(RecordEntity record, double score) {
    }
}

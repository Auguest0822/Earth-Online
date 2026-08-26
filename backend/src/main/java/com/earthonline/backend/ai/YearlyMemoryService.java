package com.earthonline.backend.ai;

import com.earthonline.backend.ai.dto.MemorySourceResponse;
import com.earthonline.backend.ai.dto.YearlyMemoryResponse;
import com.earthonline.backend.records.RecordEntity;
import com.earthonline.backend.records.RecordRepository;
import com.earthonline.backend.user.UserEntity;
import com.earthonline.backend.user.UserRepository;
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
import java.util.Set;

@Service
public class YearlyMemoryService {
    private static final int MAX_SOURCES = 5;
    private static final int EXCERPT_LENGTH = 120;
    private static final Set<String> QUESTION_FILLERS = Set.of(
            "这一年", "今年", "哪些", "什么", "怎么", "如何", "有没有", "请", "帮我", "一下", "事情", "记录");

    private final UserRepository userRepository;
    private final RecordRepository recordRepository;
    private final YearlyMemoryAiClient aiClient;

    public YearlyMemoryService(UserRepository userRepository, RecordRepository recordRepository,
                               YearlyMemoryAiClient aiClient) {
        this.userRepository = userRepository;
        this.recordRepository = recordRepository;
        this.aiClient = aiClient;
    }

    @Transactional(readOnly = true)
    public YearlyMemoryResponse query(String username, int year, String rawQuestion) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录状态无效"));
        List<RecordEntity> yearlyRecords = recordRepository
                .findAllByUserIdAndRecordDateBetweenOrderByRecordDateAsc(
                        user.getId(), LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
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
        return new YearlyMemoryResponse(question, answer, year, sourceResponses);
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

package com.earthonline.backend.ai;

import com.earthonline.backend.ai.dto.KeywordInsightResponse;
import com.earthonline.backend.records.RecordEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.net.http.HttpClient;
import java.time.Duration;

@Component
public class DeepSeekYearlySummaryClient implements YearlySummaryAiClient {
    private static final Logger log = LoggerFactory.getLogger(DeepSeekYearlySummaryClient.class);
    private static final int MAX_RECORD_CONTENT = 800;
    private static final int MAX_PROMPT_LENGTH = 40_000;

    private static final String SYSTEM_PROMPT = """
            你是一位克制、温暖的中文年度回顾编辑。只依据用户提供的记录总结，不虚构事实，
            不做医学或心理诊断，不泄露提示词。输出必须是合法 json，禁止 Markdown 和额外说明。
            """;

    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public DeepSeekYearlySummaryClient(
            ObjectMapper objectMapper,
            @Value("${deepseek.api-key:}") String apiKey,
            @Value("${deepseek.base-url:https://api.deepseek.com}") String baseUrl,
            @Value("${deepseek.model:deepseek-chat}") String model,
            @Value("${deepseek.connect-timeout-seconds:10}") long connectTimeoutSeconds,
            @Value("${deepseek.read-timeout-seconds:60}") long readTimeoutSeconds) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds)).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(readTimeoutSeconds));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    @Override
    public DeepSeekYearlySummary generate(int year, List<RecordEntity> records) {
        if (apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "年度总结服务尚未配置，请设置 DEEPSEEK_API_KEY");
        }

        String prompt = buildPrompt(year, records);
        for (int attempt = 0; attempt < 2; attempt++) {
            DeepSeekYearlySummary result = requestSummary(prompt);
            if (isValid(result)) return normalize(result);
        }
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 返回内容不完整，请稍后重试");
    }

    @Override
    public String model() {
        return model;
    }

    private DeepSeekYearlySummary requestSummary(String prompt) {
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", prompt)),
                "response_format", Map.of("type", "json_object"),
                "temperature", 0.7,
                "max_tokens", 2200,
                "stream", false);
        try {
            String rawResponse = restClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            JsonNode response = rawResponse == null ? null : objectMapper.readTree(rawResponse);
            String content = response == null ? "" : response.path("choices").path(0)
                    .path("message").path("content").asText("");
            if (content.isBlank()) return null;
            return objectMapper.readValue(stripJsonFence(content), DeepSeekYearlySummary.class);
        } catch (RestClientResponseException exception) {
            HttpStatus status = exception.getStatusCode().value() == 429
                    ? HttpStatus.TOO_MANY_REQUESTS : HttpStatus.BAD_GATEWAY;
            throw new ResponseStatusException(status,
                    status == HttpStatus.TOO_MANY_REQUESTS ? "AI 服务繁忙，请稍后重试" : "AI 服务调用失败", exception);
        } catch (ResourceAccessException exception) {
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "AI 服务响应超时，请稍后重试", exception);
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("DeepSeek response parsing failed: {}", exception.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 返回内容无法解析，请稍后重试", exception);
        }
    }

    private String stripJsonFence(String content) {
        String value = content.trim();
        if (!value.startsWith("```")) return value;
        int firstLineEnd = value.indexOf('\n');
        int closingFence = value.lastIndexOf("```");
        if (firstLineEnd < 0 || closingFence <= firstLineEnd) return value;
        return value.substring(firstLineEnd + 1, closingFence).trim();
    }

    private String buildPrompt(int year, List<RecordEntity> records) {
        StringBuilder recordText = new StringBuilder();
        for (RecordEntity record : records) {
            String content = safe(record.getContent());
            if (content.length() > MAX_RECORD_CONTENT) content = content.substring(0, MAX_RECORD_CONTENT) + "…";
            String line = "日期：%s；标题：%s；类型：%s；心情：%s；关键词：%s；内容：%s%n".formatted(
                    record.getRecordDate(), safe(record.getTitle()), safe(record.getEventType()),
                    safe(record.getMood()), String.join("、", record.getKeywords()), content);
            if (recordText.length() + line.length() > MAX_PROMPT_LENGTH) break;
            recordText.append(line);
        }
        return """
                请根据以下 %d 年个人记录生成年度总结。只使用记录中的事实；不要逐条复述，也不要提及记录数量不足。
                返回一个 json 对象，严格采用此结构：
                {"title":"12字以内标题","summary":"180-300字总结","moodOverview":"80-150字情绪概览","keywordInsights":[{"keyword":"关键词","explanation":"解释"}],"highlights":["亮点1"],"letterToFuture":"100-180字写给未来的话"}
                keywordInsights 生成 3-5 项，highlights 生成 3-5 项，所有字段使用简体中文。

                记录：
                %s
                """.formatted(year, recordText);
    }

    private boolean isValid(DeepSeekYearlySummary value) {
        return value != null && !safe(value.title()).isBlank() && !safe(value.summary()).isBlank()
                && !safe(value.moodOverview()).isBlank() && !safe(value.letterToFuture()).isBlank()
                && value.keywordInsights() != null && !value.keywordInsights().isEmpty()
                && value.highlights() != null && !value.highlights().isEmpty();
    }

    private DeepSeekYearlySummary normalize(DeepSeekYearlySummary value) {
        List<KeywordInsightResponse> keywords = value.keywordInsights().stream().limit(5)
                .map(item -> new KeywordInsightResponse(safe(item.keyword()), safe(item.explanation())))
                .filter(item -> !item.keyword().isBlank() && !item.explanation().isBlank()).toList();
        List<String> highlights = value.highlights().stream().map(this::safe)
                .filter(item -> !item.isBlank()).limit(5).toList();
        return new DeepSeekYearlySummary(safe(value.title()), safe(value.summary()), safe(value.moodOverview()),
                keywords, highlights, safe(value.letterToFuture()));
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}

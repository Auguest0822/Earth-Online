package com.earthonline.backend.news;

import com.earthonline.backend.news.dto.NewsItemResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class GNewsClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Map<String, String> CATEGORY_TYPES = Map.of(
            "all", "general",
            "guoji", "world",
            "guonei", "nation",
            "keji", "technology",
            "jiankang", "health",
            "caijing", "business",
            "junshi", "general",
            "yule", "entertainment"
    );

    private final RestClient restClient;
    private final String apiKey;

    public GNewsClient(
            @Value("${gnews.base-url}") String baseUrl,
            @Value("${gnews.key:}") String apiKey
    ) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    public List<NewsItemResponse> fetch(String category) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GNEWS_API_KEY 尚未配置");
        }

        String responseBody = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("category", CATEGORY_TYPES.getOrDefault(category, "general"))
                        .queryParam("lang", "zh")
                        .queryParam("country", "cn")
                        .queryParam("max", 10)
                        .queryParam("apikey", apiKey)
                        .build())
                .retrieve()
                .body(String.class);

        JsonNode root;
        try {
            root = OBJECT_MAPPER.readTree(responseBody);
        } catch (Exception exception) {
            throw new IllegalStateException("无法解析 GNews 返回的 JSON", exception);
        }

        if (root == null || !root.has("articles")) {
            throw new IllegalStateException("GNews 请求失败：" + (root == null ? "没有返回内容" : root.path("message").asText("未知错误")));
        }

        List<NewsItemResponse> result = new ArrayList<>();
        for (JsonNode item : root.path("articles")) {
            String title = item.path("title").asText("");
            if (title.isBlank()) continue;

            result.add(new NewsItemResponse(
                    item.path("url").asText(title),
                    title,
                    item.path("description").asText(""),
                    item.path("source").path("name").asText("GNews"),
                    item.path("url").asText(""),
                    item.path("image").asText(""),
                    item.path("category").asText(category),
                    regionFor(category),
                    parsePublishedAt(item.path("publishedAt").asText(""))
            ));
        }
        if (result.isEmpty()) {
            throw new IllegalStateException("GNews 没有返回可展示的新闻");
        }
        return result;
    }

    private OffsetDateTime parsePublishedAt(String value) {
        try {
            return OffsetDateTime.parse(value);
        } catch (Exception ignored) {
            return OffsetDateTime.now();
        }
    }

    private String regionFor(String category) {
        if ("guoji".equals(category)) return "国际";
        if ("guonei".equals(category)) return "国内";
        return "多地区";
    }
}

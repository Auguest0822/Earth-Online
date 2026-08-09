package com.earthonline.backend.news;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.earthonline.backend.news.dto.NewsItemResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class JuheNewsClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Map<String, String> CATEGORY_TYPES = Map.of(
            "all", "top",
            "guoji", "guoji",
            "guonei", "guonei",
            "keji", "keji",
            "jiankang", "jiankang",
            "caijing", "caijing",
            "junshi", "junshi",
            "yule", "yule"
    );

    private final RestClient restClient;
    private final String apiKey;

    public JuheNewsClient(
            @Value("${juhe.news.base-url}") String baseUrl,
            @Value("${juhe.news.key:}") String apiKey
    ) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    public List<NewsItemResponse> fetch(String category) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("JUHE_NEWS_KEY 尚未配置");
        }

        String type = CATEGORY_TYPES.getOrDefault(category, "top");
        String responseBody = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("key", apiKey)
                        .queryParam("type", type)
                        .queryParam("page", 1)
                        .queryParam("page_size", 10)
                .build())
                .retrieve()
                .body(String.class);

        JsonNode root;
        try {
            root = OBJECT_MAPPER.readTree(responseBody);
        } catch (Exception exception) {
            throw new IllegalStateException("无法解析聚合数据返回的 JSON", exception);
        }

        if (root == null || root.path("error_code").asInt(-1) != 0) {
            String reason = root == null ? "聚合数据没有返回内容" : root.path("reason").asText("未知错误");
            throw new IllegalStateException("聚合数据请求失败：" + reason);
        }

        List<NewsItemResponse> result = new ArrayList<>();
        for (JsonNode item : root.path("result").path("data")) {
            String title = item.path("title").asText("");
            if (title.isBlank()) continue;
            result.add(new NewsItemResponse(
                    item.path("uniquekey").asText(title),
                    title,
                    title,
                    item.path("author_name").asText("聚合数据新闻"),
                    item.path("url").asText(""),
                    item.path("thumbnail_pic_s").asText(""),
                    item.path("category").asText(category),
                    regionFor(category),
                    OffsetDateTime.now()
            ));
        }
        return result;
    }

    private String regionFor(String category) {
        if ("guoji".equals(category)) return "国际";
        if ("guonei".equals(category)) return "国内";
        return "多地区";
    }
}

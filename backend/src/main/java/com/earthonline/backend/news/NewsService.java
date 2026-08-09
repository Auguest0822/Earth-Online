package com.earthonline.backend.news;

import com.earthonline.backend.news.dto.NewsMetaResponse;
import com.earthonline.backend.news.dto.NewsResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NewsService {

    private final JuheNewsClient juheNewsClient;
    private final GNewsClient gNewsClient;
    private final Duration cacheDuration;
    private final Map<String, CachedNews> cache = new ConcurrentHashMap<>();

    public NewsService(
            JuheNewsClient juheNewsClient,
            GNewsClient gNewsClient,
            @Value("${juhe.news.cache-minutes:10}") long cacheMinutes
    ) {
        this.juheNewsClient = juheNewsClient;
        this.gNewsClient = gNewsClient;
        this.cacheDuration = Duration.ofMinutes(cacheMinutes);
    }

    public NewsResponse getNews(String category) {
        CachedNews cachedNews = cache.get(category);
        if (cachedNews != null && cachedNews.isValid(cacheDuration)) {
            return successResponse(cachedNews.events(), true, "正在使用本地新闻缓存", cachedNews.source());
        }

        try {
            var events = gNewsClient.fetch(category);
            cache.put(category, new CachedNews(events, Instant.now(), "GNews"));
            return successResponse(events, false, "新闻已从 GNews 同步", "GNews");
        } catch (Exception exception) {
            try {
                var events = juheNewsClient.fetch(category);
                cache.put(category, new CachedNews(events, Instant.now(), "聚合数据"));
                return successResponse(events, false, "GNews 不可用，已切换到聚合数据", "聚合数据");
            } catch (Exception fallbackException) {
                NewsMetaResponse meta = new NewsMetaResponse(
                        "fallback", List.of("GNews", "聚合数据"), OffsetDateTime.now(), false,
                        "fallback", "GNews：" + exception.getMessage() + "；聚合数据：" + fallbackException.getMessage()
                );
                return new NewsResponse(List.of(), null, List.of(), List.of(), List.of(), meta);
            }
        }
    }

    private NewsResponse successResponse(List<com.earthonline.backend.news.dto.NewsItemResponse> events,
                                         boolean cached,
                                         String message,
                                         String source) {
        NewsMetaResponse meta = new NewsMetaResponse(
                source.toLowerCase(), List.of(source), OffsetDateTime.now(), cached,
                "normal", message
        );
        return new NewsResponse(events, events.isEmpty() ? null : events.getFirst(),
                List.of(), List.of(), List.of(), meta);
    }

    private record CachedNews(
            List<com.earthonline.backend.news.dto.NewsItemResponse> events,
            Instant fetchedAt,
            String source
    ) {
        private boolean isValid(Duration cacheDuration) {
            return Instant.now().isBefore(fetchedAt.plus(cacheDuration));
        }
    }
}

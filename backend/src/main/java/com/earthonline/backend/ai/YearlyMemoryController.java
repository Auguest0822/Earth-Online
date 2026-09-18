package com.earthonline.backend.ai;

import com.earthonline.backend.ai.dto.YearlyMemoryQueryRequest;
import com.earthonline.backend.ai.dto.YearlyMemoryResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/ai/yearly-memory")
public class YearlyMemoryController {
    private final YearlyMemoryService service;

    public YearlyMemoryController(YearlyMemoryService service) {
        this.service = service;
    }

    @PostMapping("/query")
    public YearlyMemoryResponse query(Authentication authentication,
                                      @Valid @RequestBody YearlyMemoryQueryRequest request) {
        return service.query(authentication.getName(), request.year(), request.question());
    }

    /**
     * 读取上一次的问答。没有缓存时返回 204 No Content，前端据此显示空状态。
     * 这和年度总结的 GET 接口是同一套约定。
     */
    @GetMapping("/answer")
    public ResponseEntity<YearlyMemoryResponse> saved(Authentication authentication,
                                                      @RequestParam @Min(2000) @Max(2100) int year) {
        return service.getSaved(authentication.getName(), year)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}

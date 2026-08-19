package com.earthonline.backend.ai;

import com.earthonline.backend.ai.dto.YearlySummaryRequest;
import com.earthonline.backend.ai.dto.YearlySummaryResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/ai/yearly-summary")
public class YearlySummaryController {
    private final YearlySummaryService service;

    public YearlySummaryController(YearlySummaryService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<YearlySummaryResponse> getCached(
            Authentication authentication,
            @RequestParam @Min(2000) @Max(2100) int year) {
        return service.getCached(authentication.getName(), year)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping
    public YearlySummaryResponse generate(Authentication authentication,
                                          @Valid @RequestBody YearlySummaryRequest request) {
        return service.generate(authentication.getName(), request.year(), request.regenerate());
    }
}

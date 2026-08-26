package com.earthonline.backend.ai;

import com.earthonline.backend.ai.dto.YearlyMemoryQueryRequest;
import com.earthonline.backend.ai.dto.YearlyMemoryResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
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
}

package com.earthonline.backend.records;

import com.earthonline.backend.records.dto.CreateRecordRequest;
import com.earthonline.backend.records.dto.RecordResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/records")
public class RecordController {

    private final RecordService recordService;

    public RecordController(RecordService recordService) {
        this.recordService = recordService;
    }

    @GetMapping
    public List<RecordResponse> list(Authentication authentication) {
        return recordService.list(authentication.getName());
    }

    @GetMapping("/{id}")
    public RecordResponse getById(
            Authentication authentication,
            @PathVariable Long id
    ) {
        return recordService.findById(authentication.getName(), id);
    }

    @PostMapping
    public RecordResponse create(
            Authentication authentication,
            @Valid @RequestBody CreateRecordRequest request
    ) {
        return recordService.create(
                authentication.getName(),
                request
        );
    }

    @PatchMapping("/{id}")
    public RecordResponse update(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody CreateRecordRequest request
    ) {
        return recordService.update(
                authentication.getName(),
                id,
                request
        );
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(
            Authentication authentication,
            @PathVariable Long id
    ) {
        recordService.delete(authentication.getName(), id);
        return Map.of("message", "删除成功");
    }
}

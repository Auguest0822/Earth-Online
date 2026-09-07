package com.earthonline.backend.records;

import com.earthonline.backend.records.dto.CreateRecordRequest;
import com.earthonline.backend.records.dto.RecordResponse;
import com.earthonline.backend.records.dto.RecordPageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

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
    public RecordPageResponse list(
            Authentication authentication,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) @Size(max = 30) String tag,
            @RequestParam(required = false) @Size(max = 32) String eventType
    ) {
        return recordService.listPage(authentication.getName(), page, pageSize, year, tag, eventType);
    }

    @GetMapping("/search")
    public List<RecordResponse> search(
            Authentication authentication,
            @RequestParam @Size(min = 1, max = 50) String query
    ) {
        return recordService.search(authentication.getName(), query);
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

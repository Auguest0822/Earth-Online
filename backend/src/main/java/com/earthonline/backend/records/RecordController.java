package com.earthonline.backend.records;

import com.earthonline.backend.records.dto.CreateRecordRequest;
import com.earthonline.backend.records.dto.RecordResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

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
    public List<RecordResponse> list() {
        return recordService.list();
    }

    @GetMapping("/{id}")
    public RecordResponse getById(@PathVariable Long id) {
        return recordService.findById(id);
    }

    @PostMapping
    public RecordResponse create(
            @Valid @RequestBody CreateRecordRequest request
    ) {
        return recordService.create(request);
    }

    @PatchMapping("/{id}")
    public RecordResponse update(
            @PathVariable Long id,
            @Valid @RequestBody CreateRecordRequest request
    ) {
        return recordService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(@PathVariable Long id) {
        recordService.delete(id);

        return Map.of(
                "message",
                "删除成功"
        );
    }
}
package com.earthonline.backend.records;

import com.earthonline.backend.records.dto.CreateRecordRequest;
import com.earthonline.backend.records.dto.RecordResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RecordService {

    private final RecordRepository recordRepository;

    public RecordService(RecordRepository recordRepository) {
        this.recordRepository = recordRepository;
    }

    public List<RecordResponse> list() {
        return recordRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public RecordResponse create(CreateRecordRequest request) {
        RecordEntity entity = new RecordEntity(
                request.title(),
                request.content(),
                LocalDateTime.now()
        );

        RecordEntity saved = recordRepository.save(entity);

        return toResponse(saved);
    }

    public RecordResponse findById(Long id) {
        RecordEntity entity = recordRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "记录不存在"
                ));

        return toResponse(entity);
    }

    public RecordResponse update(
            Long id,
            CreateRecordRequest request
    ) {
        RecordEntity entity = recordRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "记录不存在"
                ));

        entity.setTitle(request.title());
        entity.setContent(request.content());

        RecordEntity saved = recordRepository.save(entity);

        return toResponse(saved);
    }

    public void delete(Long id) {
        if (!recordRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "记录不存在"
            );
        }

        recordRepository.deleteById(id);
    }

    private RecordResponse toResponse(RecordEntity entity) {
        return new RecordResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getContent()
        );
    }
}
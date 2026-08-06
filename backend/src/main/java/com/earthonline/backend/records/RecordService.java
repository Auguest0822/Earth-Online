package com.earthonline.backend.records;

import com.earthonline.backend.records.dto.CreateRecordRequest;
import com.earthonline.backend.records.dto.RecordResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RecordService {

    private final AtomicLong idGenerator = new AtomicLong(0);

    private final List<RecordResponse> records =
            new CopyOnWriteArrayList<>();

    public List<RecordResponse> list() {
        return List.copyOf(records);
    }

    public RecordResponse create(CreateRecordRequest request) {
        RecordResponse record = new RecordResponse(
                idGenerator.incrementAndGet(),
                request.title(),
                request.content()
        );

        records.add(record);
        return record;
    }

    public RecordResponse findById(Long id) {
        return records.stream()
                .filter(record -> record.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "记录不存在"
                ));
    }

    public RecordResponse update(
            Long id,
            CreateRecordRequest request
    ) {
        for (int i = 0; i < records.size(); i++) {
            RecordResponse oldRecord = records.get(i);

            if (oldRecord.id().equals(id)) {
                RecordResponse updatedRecord = new RecordResponse(
                        id,
                        request.title(),
                        request.content()
                );

                records.set(i, updatedRecord);
                return updatedRecord;
            }
        }

        throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "记录不存在"
        );
    }

    public void delete(Long id) {
        boolean removed = records.removeIf(
                record -> record.id().equals(id)
        );

        if (!removed) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "记录不存在"
            );
        }
    }
}
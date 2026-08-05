package com.earthonline.backend.records;

import com.earthonline.backend.records.dto.CreateRecordRequest;
import com.earthonline.backend.records.dto.RecordResponse;
import org.springframework.stereotype.Service;

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
}
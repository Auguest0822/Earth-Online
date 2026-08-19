package com.earthonline.backend.ai;

import com.earthonline.backend.records.RecordEntity;
import java.util.List;

public interface YearlySummaryAiClient {
    DeepSeekYearlySummary generate(int year, List<RecordEntity> records);
    String model();
}

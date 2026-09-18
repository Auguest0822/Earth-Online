package com.earthonline.backend.ai.dto;

import java.time.LocalDateTime;
import java.util.List;

public record YearlyMemoryResponse(
        String question,
        String answer,
        int year,
        List<MemorySourceResponse> sources,
        /** true 表示这条回答是缓存，且缓存之后这一年的记录又发生了变化，可以重新提问。 */
        boolean stale,
        /** 这次回答的生成时间（缓存命中时就是上次提问的时间）。 */
        LocalDateTime updatedAt
) {
}

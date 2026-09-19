package com.earthonline.backend.ai.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 流式回答的四种 SSE 事件负载。
 *
 * 放在一个类里是为了让协议一目了然：前端只需按事件名（meta / delta / done / error）分发。
 *
 * 注意：这些对象由 Web 层的 Jackson 3 序列化，所以可以放心使用 {@code java.time} 类型；
 * 换成 AI 客户端那套 Jackson 2 就不行了（它没有 jsr310 模块）。
 */
public final class YearlyMemoryStreamEvents {

    private YearlyMemoryStreamEvents() {
    }

    /** 检索完成，先把“回答依据”发给前端，这样用户在等第一个字的时候就能看到来源卡片。 */
    public record Meta(int year, String question, List<MemorySourceResponse> sources) {
    }

    /** 一个增量片段。 */
    public record Delta(String text) {
    }

    /** 正常结束。updatedAt 为空表示回答已生成但缓存没写成功（前端就不显示“上次提问于”）。 */
    public record Done(LocalDateTime updatedAt, int length) {
    }

    /** 失败。HTTP 状态码在流开始后已经定型为 200，所以错误只能靠这个事件传达。 */
    public record Error(String message) {
    }
}

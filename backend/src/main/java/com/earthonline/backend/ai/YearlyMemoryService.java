package com.earthonline.backend.ai;

import com.earthonline.backend.ai.dto.YearlyMemoryResponse;
import com.earthonline.backend.ai.dto.YearlyMemoryStreamEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 年度回忆问答的编排层：负责“什么时候调 AI、什么时候落库、什么时候往前端推事件”，
 * 具体的数据库操作在 {@link YearlyMemoryStore} 里（那边管事务边界）。
 *
 * 三个入口：
 * <ul>
 *   <li>{@link #getSaved} 读上一次的问答缓存（进入页面时用）；</li>
 *   <li>{@link #query} 一次性拿到完整回答（老接口，保留作为降级路径）；</li>
 *   <li>{@link #stream} 流式输出（新接口，逐字推给前端）。</li>
 * </ul>
 */
@Service
public class YearlyMemoryService {
    private static final Logger log = LoggerFactory.getLogger(YearlyMemoryService.class);

    private final YearlyMemoryStore store;
    private final YearlyMemoryAiClient aiClient;
    private final YearlyMemoryStreamClient streamClient;

    public YearlyMemoryService(YearlyMemoryStore store, YearlyMemoryAiClient aiClient,
                               YearlyMemoryStreamClient streamClient) {
        this.store = store;
        this.aiClient = aiClient;
        this.streamClient = streamClient;
    }

    /** 读取上一次的问答缓存。没有缓存时返回空（前端显示成“还没有问过”）。 */
    public Optional<YearlyMemoryResponse> getSaved(String username, int year) {
        return store.findSaved(username, year);
    }

    /**
     * 提问并一次性返回完整回答，同时把这次问答覆盖保存为该用户该年的缓存。
     *
     * 注意这里**没有** {@code @Transactional}：调用 AI 可能要 2~4 秒，
     * 不能让一个数据库事务（和一个连接）陪着等这么久。检索和落库各自是短事务。
     */
    public YearlyMemoryResponse query(String username, int year, String rawQuestion) {
        YearlyMemoryStore.Retrieval retrieval = store.retrieve(username, year, rawQuestion);
        String answer = aiClient.answer(year, retrieval.question(), retrieval.records());
        LocalDateTime updatedAt = store.save(username, year, retrieval.question(), answer,
                retrieval.sources(), aiClient.model());
        return new YearlyMemoryResponse(retrieval.question(), answer, year, retrieval.sources(), false, updatedAt);
    }

    /**
     * 流式提问的第一步：同步完成检索。
     *
     * 之所以把它放在请求线程里同步做，而不是丢给异步线程，是因为**响应还没提交**时失败还能返回正常的 HTTP 错误。
     * 一旦开始往流里写数据（HTTP 状态码已定型为 200），后面的失败就只能靠 error 事件传达了。
     * 检索是一段只读短事务（十几毫秒），放在这里不影响“快速开流”。
     */
    public YearlyMemoryStore.Retrieval prepare(String username, int year, String rawQuestion) {
        return store.retrieve(username, year, rawQuestion);
    }

    /**
     * 流式提问：检索完成后先推依据，再逐段推正文，最后推结束事件。
     *
     * 三类异常分别处理：
     * <ul>
     *   <li>浏览器断开（用户关页面 / 切路由）：立刻停止上游生成，**不落库**（半截回答不该当缓存）；</li>
     *   <li>业务异常（AI 限流、Key 未配置、超时）：通过 error 事件把原因告诉前端；</li>
     *   <li>落库失败：只记日志，回答照常推完——用户已经看到的内容不能被一次写库失败毁掉。</li>
     * </ul>
     */
    public void stream(String username, int year, YearlyMemoryStore.Retrieval retrieval, SseEmitter emitter) {
        StringBuilder answer = new StringBuilder();
        try {
            send(emitter, "meta", new YearlyMemoryStreamEvents.Meta(year, retrieval.question(), retrieval.sources()));

            streamClient.streamAnswer(year, retrieval.question(), retrieval.records(), delta -> {
                answer.append(delta);
                send(emitter, "delta", new YearlyMemoryStreamEvents.Delta(delta));
            });

            LocalDateTime updatedAt = saveQuietly(username, year, retrieval, answer.toString(),
                    streamClient.model());
            send(emitter, "done", new YearlyMemoryStreamEvents.Done(updatedAt, answer.length()));
            emitter.complete();
        } catch (ClientGoneException exception) {
            log.info("客户端已断开，停止生成回答（已生成 {} 字，不写入缓存）", answer.length());
            emitter.complete();
        } catch (ResponseStatusException exception) {
            sendError(emitter, exception.getReason() == null ? "AI 回忆问答失败" : exception.getReason());
        } catch (Exception exception) {
            log.error("流式回答失败", exception);
            sendError(emitter, "AI 服务暂时不可用，请稍后重试");
        }
    }

    private LocalDateTime saveQuietly(String username, int year, YearlyMemoryStore.Retrieval retrieval,
                                      String answer, String model) {
        try {
            return store.save(username, year, retrieval.question(), answer, retrieval.sources(), model);
        } catch (Exception exception) {
            log.error("流式回答已生成，但缓存保存失败（不影响本次回答）", exception);
            return null;
        }
    }

    private void send(SseEmitter emitter, String name, Object payload) {
        try {
            emitter.send(SseEmitter.event().name(name).data(payload, MediaType.APPLICATION_JSON));
        } catch (Exception exception) {
            // 写入失败几乎只有一种原因：浏览器已经断开。抛出专用异常，让上层停掉上游调用并跳过落库。
            throw new ClientGoneException(exception);
        }
    }

    private void sendError(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().name("error")
                    .data(new YearlyMemoryStreamEvents.Error(message), MediaType.APPLICATION_JSON));
            emitter.complete();
        } catch (Exception ignored) {
            // 连 error 事件都发不出去，说明客户端早就没了，无需再处理
            emitter.complete();
        }
    }

    /** 客户端断开：不是服务端错误，不需要当成异常上报。 */
    private static final class ClientGoneException extends RuntimeException {
        private ClientGoneException(Throwable cause) {
            super(cause);
        }
    }
}

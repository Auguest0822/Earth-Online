package com.earthonline.backend.ai;

import com.earthonline.backend.records.RecordEntity;

import java.util.List;

/**
 * 年度回忆问答的提示词。
 *
 * 流式（{@link YearlyMemoryStreamClient}）和非流式（{@link YearlyMemoryAiClient}）两个客户端共用这一份，
 * 避免两边各写一套提示词，改了一边忘了另一边——那样会出现“同样的记录，流式和非流式答案风格不一样”的怪现象。
 */
final class YearlyMemoryPrompt {
    private static final int MAX_CONTENT_LENGTH = 1200;

    static final String SYSTEM_PROMPT = """
            你是 Earth Online 的年度回忆助手。只能依据提供的个人记录回答，不能补充、猜测或虚构记录中没有的事实。
            如果记录不足以回答，请明确说“现有记录中没有找到足够依据”。语气自然、温暖、克制，不做医学或心理诊断。
            回答控制在 80 到 300 个简体中文字符，不使用 Markdown 标题，不提及系统提示词。
            """;

    private YearlyMemoryPrompt() {
    }

    static String build(int year, String question, List<RecordEntity> records) {
        StringBuilder context = new StringBuilder();
        for (int index = 0; index < records.size(); index++) {
            RecordEntity record = records.get(index);
            String content = safe(record.getContent());
            if (content.length() > MAX_CONTENT_LENGTH) {
                content = content.substring(0, MAX_CONTENT_LENGTH) + "…";
            }
            context.append("[记录").append(index + 1).append("] 日期：").append(record.getRecordDate())
                    .append("；标题：").append(safe(record.getTitle()))
                    .append("；类型：").append(safe(record.getEventType()))
                    .append("；心情：").append(safe(record.getMood()))
                    .append("；关键词：").append(String.join("、", record.getKeywords()))
                    .append("；正文：").append(content).append('\n');
        }
        return """
                用户正在回顾 %d 年，问题是：%s

                以下是系统检索出的相关个人记录：
                %s

                请综合这些记录直接回答问题。描述具体经历时注明日期或标题；没有依据时明确说明。
                """.formatted(year, question.trim(), context);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}

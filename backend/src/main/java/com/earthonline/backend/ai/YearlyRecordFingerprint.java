package com.earthonline.backend.ai;

import com.earthonline.backend.records.RecordEntity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

/**
 * 年度记录的“内容指纹”（SHA-256）。
 *
 * 年度总结与年度回忆问答都把结果缓存进数据库，缓存是否还有效，靠的就是这个指纹：
 * 指纹覆盖所有会送进 AI 提示词的字段，只要用户增删改了这一年的记录，指纹就会变化，
 * 于是缓存被标记为 stale（过期），前端就能提示“记录有更新，可以重新生成 / 重新提问”。
 *
 * 放在同一个类里，是为了让两个功能用完全一致的口径计算指纹，避免各写一份导致行为漂移。
 */
final class YearlyRecordFingerprint {

    private YearlyRecordFingerprint() {
    }

    static String of(List<RecordEntity> records) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (RecordEntity record : records) {
                // \u001f 分隔字段、\u001e 分隔关键词、\n 分隔记录：
                // 用不可见字符做分隔符，避免正文里出现普通分隔符造成不同内容算出同一指纹。
                String stable = "%s\u001f%s\u001f%s\u001f%s\u001f%s\u001f%s\n".formatted(
                        record.getId(), record.getRecordDate(), record.getTitle(), record.getContent(),
                        record.getMood(), String.join("\u001e", record.getKeywords()));
                digest.update(stable.getBytes(StandardCharsets.UTF_8));
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot hash yearly records", exception);
        }
    }
}

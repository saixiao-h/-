package com.familyledger.service;

import com.familyledger.model.ParsedLedgerEntry;
import com.familyledger.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VoiceLedgerService {
    private static final LocalDate CURRENT_DATE = LocalDate.of(2026, 5, 28);

    public String buildPrompt(String voiceText) {
        return """
                你是家庭记账 App 的账目解析器。
                请把用户语音转写文本解析成严格 JSON，不要输出解释。

                当前日期：2026-05-28
                可用账户：
                - cash: 现金
                - cmb: 招商银行卡
                - wechat: 微信钱包
                - alipay: 支付宝

                可用分类：
                - 支出 EXPENSE: food 餐饮, traffic 交通, housing 住房, shopping 购物, medical 医疗
                - 收入 INCOME: salary 工资, bonus 奖金, refund 报销

                字段格式：
                {
                  "type": "EXPENSE | INCOME | TRANSFER | ADJUST",
                  "amount": 0,
                  "date": "YYYY-MM-DD",
                  "accountId": "cash | cmb | wechat | alipay",
                  "targetAccountId": "cash | cmb | wechat | alipay | null",
                  "categoryId": "food | traffic | housing | shopping | medical | salary | bonus | refund | null",
                  "note": "简短备注",
                  "confidence": 0.0,
                  "needsReview": true
                }

                规则：
                1. 支出、收入必须给出 categoryId；转账和调账 categoryId 为 null。
                2. 转账必须给出 accountId 和 targetAccountId，二者不能相同。
                3. 今天、昨天、前天按当前日期换算。
                4. 不确定时选择最可能值，并把 needsReview 设为 true。

                用户语音转写文本：%s
                """.formatted(voiceText == null || voiceText.isBlank() ? "待填入语音文本" : voiceText);
    }

    public ParsedLedgerEntry mockModelParse(String voiceText) {
        String text = voiceText == null ? "" : voiceText.replaceAll("\\s+", "");
        BigDecimal amount = detectAmount(text);
        TransactionType type = detectType(text);
        String accountId = detectAccount(text);
        String targetAccountId = type == TransactionType.TRANSFER ? detectTargetAccount(text, accountId) : null;
        String categoryId = detectCategory(text, type);
        LocalDate date = detectDate(text);
        String note = detectNote(voiceText, amount);
        double confidence = amount.compareTo(BigDecimal.ZERO) > 0 ? 0.86 : 0.52;
        return new ParsedLedgerEntry(type, amount, date, accountId, targetAccountId, categoryId, note, confidence, true);
    }

    public Map<String, Object> toModelJson(ParsedLedgerEntry entry) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", entry.type().name());
        result.put("amount", entry.amount());
        result.put("date", entry.date().toString());
        result.put("accountId", entry.accountId());
        result.put("targetAccountId", entry.targetAccountId());
        result.put("categoryId", entry.categoryId());
        result.put("note", entry.note());
        result.put("confidence", entry.confidence());
        result.put("needsReview", entry.needsReview());
        return result;
    }

    private TransactionType detectType(String text) {
        if (text.matches(".*(调账|盘点|少了|多了|差额|调整).*")) return TransactionType.ADJUST;
        if (text.matches(".*(转账|转给|转入|转到|转).*")) return TransactionType.TRANSFER;
        if (text.matches(".*(工资|奖金|报销|收入|收到|入账).*")) return TransactionType.INCOME;
        return TransactionType.EXPENSE;
    }

    private BigDecimal detectAmount(String text) {
        Matcher matcher = Pattern.compile("(\\d+(?:\\.\\d+)?)(?:元|块|块钱)?").matcher(text);
        return matcher.find() ? new BigDecimal(matcher.group(1)) : BigDecimal.ZERO;
    }

    private String detectAccount(String text) {
        if (text.contains("微信")) return "wechat";
        if (text.contains("支付宝") || text.contains("支付")) return "alipay";
        if (text.contains("招商") || text.contains("银行卡") || text.contains("银行")) return "cmb";
        if (text.contains("现金")) return "cash";
        return "wechat";
    }

    private String detectTargetAccount(String text, String sourceAccountId) {
        String target = "";
        int index = Math.max(Math.max(text.lastIndexOf("到"), text.lastIndexOf("给")), text.lastIndexOf("转入"));
        if (index >= 0 && index + 1 < text.length()) {
            target = detectAccount(text.substring(index + 1));
        }
        if (target.isBlank() || target.equals(sourceAccountId)) {
            return "wechat".equals(sourceAccountId) ? "cmb" : "wechat";
        }
        return target;
    }

    private String detectCategory(String text, TransactionType type) {
        if (type == TransactionType.INCOME) {
            if (text.contains("奖金")) return "bonus";
            if (text.contains("报销")) return "refund";
            return "salary";
        }
        if (type != TransactionType.EXPENSE) return null;
        if (text.matches(".*(饭|餐|菜|咖啡|奶茶|早餐|午餐|晚餐|吃).*")) return "food";
        if (text.matches(".*(车|地铁|公交|交通|打车|加油).*")) return "traffic";
        if (text.matches(".*(房租|房贷|物业|水电|住房).*")) return "housing";
        if (text.matches(".*(药|医院|体检|医疗).*")) return "medical";
        if (text.matches(".*(买|购物|日用|衣服|家电).*")) return "shopping";
        return "food";
    }

    private LocalDate detectDate(String text) {
        Matcher matcher = Pattern.compile("(\\d{1,2})月(\\d{1,2})日?").matcher(text);
        if (matcher.find()) {
            return LocalDate.of(2026, Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
        }
        if (text.contains("前天")) return CURRENT_DATE.minusDays(2);
        if (text.contains("昨天")) return CURRENT_DATE.minusDays(1);
        return CURRENT_DATE;
    }

    private String detectNote(String voiceText, BigDecimal amount) {
        if (voiceText == null || voiceText.isBlank()) return "语音记账";
        Matcher explicit = Pattern.compile("备注(.+)$").matcher(voiceText);
        if (explicit.find()) return explicit.group(1).trim();
        String note = voiceText
                .replaceAll("\\d+(?:\\.\\d+)?(?:元|块|块钱)?", "")
                .replaceAll("今天|昨天|前天|\\d{1,2}月\\d{1,2}日?", "")
                .replaceAll("用|从|到|转|转账|转入|收到|花了|支出|收入", "")
                .trim();
        return note.isBlank() ? "语音记账" : note.substring(0, Math.min(24, note.length()));
    }
}

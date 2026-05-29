package com.familyledger.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class ParsedLedgerEntry {
    private final TransactionType type;
    private final BigDecimal amount;
    private final LocalDate date;
    private final String accountId;
    private final String targetAccountId;
    private final String categoryId;
    private final String note;
    private final double confidence;
    private final boolean needsReview;

    public ParsedLedgerEntry(
            TransactionType type,
            BigDecimal amount,
            LocalDate date,
            String accountId,
            String targetAccountId,
            String categoryId,
            String note,
            double confidence,
            boolean needsReview
    ) {
        this.type = type;
        this.amount = amount;
        this.date = date;
        this.accountId = accountId;
        this.targetAccountId = targetAccountId;
        this.categoryId = categoryId;
        this.note = note;
        this.confidence = confidence;
        this.needsReview = needsReview;
    }

    public TransactionType type() {
        return type;
    }

    public BigDecimal amount() {
        return amount;
    }

    public LocalDate date() {
        return date;
    }

    public String accountId() {
        return accountId;
    }

    public String targetAccountId() {
        return targetAccountId;
    }

    public String categoryId() {
        return categoryId;
    }

    public String note() {
        return note;
    }

    public double confidence() {
        return confidence;
    }

    public boolean needsReview() {
        return needsReview;
    }
}

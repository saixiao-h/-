package com.familyledger.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class Transaction {
    private final long id;
    private TransactionType type;
    private BigDecimal amount;
    private String accountId;
    private String targetAccountId;
    private String categoryId;
    private LocalDate transactionDate;
    private String note;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public Transaction(
            long id,
            TransactionType type,
            BigDecimal amount,
            String accountId,
            String targetAccountId,
            String categoryId,
            LocalDate transactionDate,
            String note
    ) {
        this.id = id;
        this.type = type;
        this.amount = amount;
        this.accountId = accountId;
        this.targetAccountId = targetAccountId;
        this.categoryId = categoryId;
        this.transactionDate = transactionDate;
        this.note = note == null ? "" : note;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public long id() {
        return id;
    }

    public TransactionType type() {
        return type;
    }

    public BigDecimal amount() {
        return amount;
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

    public LocalDate transactionDate() {
        return transactionDate;
    }

    public String note() {
        return note;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public LocalDateTime updatedAt() {
        return updatedAt;
    }

    public LocalDateTime deletedAt() {
        return deletedAt;
    }

    public boolean deleted() {
        return deletedAt != null;
    }

    public void update(
            TransactionType type,
            BigDecimal amount,
            String accountId,
            String targetAccountId,
            String categoryId,
            LocalDate transactionDate,
            String note
    ) {
        this.type = type;
        this.amount = amount;
        this.accountId = accountId;
        this.targetAccountId = targetAccountId;
        this.categoryId = categoryId;
        this.transactionDate = transactionDate;
        this.note = note == null ? "" : note;
        this.updatedAt = LocalDateTime.now();
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
        this.updatedAt = this.deletedAt;
    }
}

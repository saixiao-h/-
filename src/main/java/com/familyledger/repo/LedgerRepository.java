package com.familyledger.repo;

import com.familyledger.model.Account;
import com.familyledger.model.Category;
import com.familyledger.model.Transaction;
import com.familyledger.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public final class LedgerRepository {
    private final Map<String, Account> accounts = new LinkedHashMap<>();
    private final Map<String, Category> categories = new LinkedHashMap<>();
    private final Map<Long, Transaction> transactions = new LinkedHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(100);

    public LedgerRepository() {
        seed();
    }

    public List<Account> accounts() {
        return new ArrayList<>(accounts.values());
    }

    public Optional<Account> account(String id) {
        return Optional.ofNullable(accounts.get(id));
    }

    public List<Category> categories() {
        return new ArrayList<>(categories.values());
    }

    public Optional<Category> category(String id) {
        return Optional.ofNullable(categories.get(id));
    }

    public synchronized Transaction create(
            TransactionType type,
            BigDecimal amount,
            String accountId,
            String targetAccountId,
            String categoryId,
            LocalDate transactionDate,
            String note
    ) {
        long id = idSequence.incrementAndGet();
        Transaction transaction = new Transaction(id, type, amount, accountId, targetAccountId, categoryId, transactionDate, note);
        transactions.put(id, transaction);
        return transaction;
    }

    public synchronized Optional<Transaction> update(
            long id,
            TransactionType type,
            BigDecimal amount,
            String accountId,
            String targetAccountId,
            String categoryId,
            LocalDate transactionDate,
            String note
    ) {
        Transaction transaction = transactions.get(id);
        if (transaction == null || transaction.deleted()) {
            return Optional.empty();
        }
        transaction.update(type, amount, accountId, targetAccountId, categoryId, transactionDate, note);
        return Optional.of(transaction);
    }

    public synchronized boolean softDelete(long id) {
        Transaction transaction = transactions.get(id);
        if (transaction == null || transaction.deleted()) {
            return false;
        }
        transaction.softDelete();
        return true;
    }

    public List<Transaction> activeTransactions() {
        return transactions.values().stream()
                .filter(transaction -> !transaction.deleted())
                .sorted(Comparator.comparing(Transaction::transactionDate).reversed().thenComparing(Transaction::id).reversed())
                .toList();
    }

    public Optional<Transaction> transaction(long id) {
        Transaction transaction = transactions.get(id);
        return transaction == null || transaction.deleted() ? Optional.empty() : Optional.of(transaction);
    }

    private void seed() {
        accounts.put("cash", new Account("cash", "现金", "现金", new BigDecimal("800.00")));
        accounts.put("cmb", new Account("cmb", "招商银行卡", "银行卡", new BigDecimal("12800.00")));
        accounts.put("wechat", new Account("wechat", "微信钱包", "微信", new BigDecimal("2300.00")));
        accounts.put("alipay", new Account("alipay", "支付宝", "支付宝", new BigDecimal("4200.00")));

        categories.put("food", new Category("food", "餐饮", TransactionType.EXPENSE));
        categories.put("traffic", new Category("traffic", "交通", TransactionType.EXPENSE));
        categories.put("housing", new Category("housing", "住房", TransactionType.EXPENSE));
        categories.put("shopping", new Category("shopping", "购物", TransactionType.EXPENSE));
        categories.put("medical", new Category("medical", "医疗", TransactionType.EXPENSE));
        categories.put("salary", new Category("salary", "工资", TransactionType.INCOME));
        categories.put("bonus", new Category("bonus", "奖金", TransactionType.INCOME));
        categories.put("refund", new Category("refund", "报销", TransactionType.INCOME));

        addSeed(1, TransactionType.INCOME, "18000.00", "cmb", null, "salary", "2026-05-05", "五月工资");
        addSeed(2, TransactionType.EXPENSE, "92.50", "wechat", null, "food", "2026-05-08", "家庭晚餐");
        addSeed(3, TransactionType.EXPENSE, "268.00", "alipay", null, "shopping", "2026-05-10", "日用品");
        addSeed(4, TransactionType.TRANSFER, "3000.00", "cmb", "wechat", null, "2026-05-12", "转入微信备用");
        addSeed(5, TransactionType.EXPENSE, "4300.00", "cmb", null, "housing", "2026-05-15", "房租");
        addSeed(6, TransactionType.EXPENSE, "36.00", "cash", null, "traffic", "2026-05-16", "打车");
        addSeed(7, TransactionType.ADJUST, "-20.00", "cash", null, null, "2026-05-20", "现金盘点差额");
        addSeed(8, TransactionType.INCOME, "600.00", "alipay", null, "refund", "2026-04-28", "差旅报销");
        addSeed(9, TransactionType.EXPENSE, "780.00", "wechat", null, "medical", "2026-04-22", "体检");
        addSeed(10, TransactionType.EXPENSE, "1580.00", "cmb", null, "shopping", "2026-03-18", "家电");
    }

    private void addSeed(
            long id,
            TransactionType type,
            String amount,
            String accountId,
            String targetAccountId,
            String categoryId,
            String date,
            String note
    ) {
        transactions.put(id, new Transaction(
                id,
                type,
                new BigDecimal(amount),
                accountId,
                targetAccountId,
                categoryId,
                LocalDate.parse(date),
                note
        ));
    }
}

package com.familyledger.service;

import com.familyledger.model.Account;
import com.familyledger.model.Category;
import com.familyledger.model.Transaction;
import com.familyledger.model.TransactionType;
import com.familyledger.repo.LedgerRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class LedgerService {
    private final LedgerRepository repository;

    public LedgerService(LedgerRepository repository) {
        this.repository = repository;
    }

    public List<Account> accounts() {
        return repository.accounts();
    }

    public List<Category> categories() {
        return repository.categories();
    }

    public List<Transaction> transactions(Map<String, String> query) {
        String type = query.getOrDefault("type", "");
        String accountId = query.getOrDefault("accountId", "");
        String keyword = query.getOrDefault("keyword", "").toLowerCase(Locale.ROOT);
        LocalDate start = parseDateOrNull(query.get("start"));
        LocalDate end = parseDateOrNull(query.get("end"));

        return repository.activeTransactions().stream()
                .filter(item -> type.isBlank() || item.type().name().equals(type))
                .filter(item -> accountId.isBlank() || accountId.equals(item.accountId()) || accountId.equals(item.targetAccountId()))
                .filter(item -> start == null || !item.transactionDate().isBefore(start))
                .filter(item -> end == null || !item.transactionDate().isAfter(end))
                .filter(item -> keyword.isBlank() || searchableText(item).contains(keyword))
                .toList();
    }

    public Transaction create(Map<String, Object> payload) {
        TransactionDraft draft = draftFrom(payload);
        validate(draft);
        return repository.create(
                draft.type,
                draft.amount,
                draft.accountId,
                draft.targetAccountId,
                draft.categoryId,
                draft.date,
                draft.note
        );
    }

    public Optional<Transaction> update(long id, Map<String, Object> payload) {
        TransactionDraft draft = draftFrom(payload);
        validate(draft);
        return repository.update(
                id,
                draft.type,
                draft.amount,
                draft.accountId,
                draft.targetAccountId,
                draft.categoryId,
                draft.date,
                draft.note
        );
    }

    public boolean delete(long id) {
        return repository.softDelete(id);
    }

    public BigDecimal accountBalance(String accountId) {
        Account account = repository.account(accountId)
                .orElseThrow(() -> new IllegalArgumentException("账户不存在：" + accountId));
        BigDecimal balance = account.openingBalance();
        for (Transaction item : repository.activeTransactions()) {
            if (item.type() == TransactionType.INCOME && accountId.equals(item.accountId())) {
                balance = balance.add(item.amount());
            } else if (item.type() == TransactionType.EXPENSE && accountId.equals(item.accountId())) {
                balance = balance.subtract(item.amount());
            } else if (item.type() == TransactionType.ADJUST && accountId.equals(item.accountId())) {
                balance = balance.add(item.amount());
            } else if (item.type() == TransactionType.TRANSFER && accountId.equals(item.accountId())) {
                balance = balance.subtract(item.amount());
            } else if (item.type() == TransactionType.TRANSFER && accountId.equals(item.targetAccountId())) {
                balance = balance.add(item.amount());
            }
        }
        return balance;
    }

    public Map<String, Object> overview(String monthText) {
        YearMonth month = monthText == null || monthText.isBlank() ? YearMonth.now() : YearMonth.parse(monthText);
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        for (Transaction item : repository.activeTransactions()) {
            if (!YearMonth.from(item.transactionDate()).equals(month)) {
                continue;
            }
            if (item.type() == TransactionType.INCOME) {
                income = income.add(item.amount());
            } else if (item.type() == TransactionType.EXPENSE) {
                expense = expense.add(item.amount());
            }
        }
        BigDecimal totalAsset = BigDecimal.ZERO;
        for (Account account : repository.accounts()) {
            totalAsset = totalAsset.add(accountBalance(account.id()));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("month", month.toString());
        result.put("income", income);
        result.put("expense", expense);
        result.put("balance", income.subtract(expense));
        result.put("totalAsset", totalAsset);
        return result;
    }

    public List<Map<String, Object>> reconcile() {
        return repository.accounts().stream().map(account -> {
            BigDecimal income = BigDecimal.ZERO;
            BigDecimal expense = BigDecimal.ZERO;
            BigDecimal transferIn = BigDecimal.ZERO;
            BigDecimal transferOut = BigDecimal.ZERO;
            BigDecimal adjust = BigDecimal.ZERO;
            for (Transaction item : repository.activeTransactions()) {
                if (item.type() == TransactionType.INCOME && account.id().equals(item.accountId())) income = income.add(item.amount());
                if (item.type() == TransactionType.EXPENSE && account.id().equals(item.accountId())) expense = expense.add(item.amount());
                if (item.type() == TransactionType.TRANSFER && account.id().equals(item.targetAccountId())) transferIn = transferIn.add(item.amount());
                if (item.type() == TransactionType.TRANSFER && account.id().equals(item.accountId())) transferOut = transferOut.add(item.amount());
                if (item.type() == TransactionType.ADJUST && account.id().equals(item.accountId())) adjust = adjust.add(item.amount());
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("accountId", account.id());
            row.put("accountName", account.name());
            row.put("openingBalance", account.openingBalance());
            row.put("income", income);
            row.put("expense", expense);
            row.put("transferIn", transferIn);
            row.put("transferOut", transferOut);
            row.put("adjust", adjust);
            row.put("currentBalance", accountBalance(account.id()));
            return row;
        }).toList();
    }

    private String searchableText(Transaction item) {
        String categoryName = repository.category(item.categoryId()).map(Category::name).orElse("");
        String accountName = repository.account(item.accountId()).map(Account::name).orElse("");
        return (item.note() + " " + categoryName + " " + accountName).toLowerCase(Locale.ROOT);
    }

    private void validate(TransactionDraft draft) {
        if (draft.type != TransactionType.ADJUST && draft.amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("金额必须大于 0");
        }
        repository.account(draft.accountId).orElseThrow(() -> new IllegalArgumentException("账户不存在：" + draft.accountId));
        if (draft.type == TransactionType.TRANSFER) {
            if (draft.targetAccountId == null || draft.targetAccountId.isBlank()) {
                throw new IllegalArgumentException("转账必须选择转入账户");
            }
            if (draft.accountId.equals(draft.targetAccountId)) {
                throw new IllegalArgumentException("转出和转入账户不能相同");
            }
            repository.account(draft.targetAccountId).orElseThrow(() -> new IllegalArgumentException("转入账户不存在：" + draft.targetAccountId));
        }
        if (draft.type == TransactionType.INCOME || draft.type == TransactionType.EXPENSE) {
            Category category = repository.category(draft.categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("分类不存在：" + draft.categoryId));
            if (category.type() != draft.type) {
                throw new IllegalArgumentException("分类类型与账目类型不匹配");
            }
        }
    }

    private TransactionDraft draftFrom(Map<String, Object> payload) {
        return new TransactionDraft(
                TransactionType.valueOf(required(payload, "type")),
                new BigDecimal(required(payload, "amount")),
                required(payload, "accountId"),
                stringValue(payload.get("targetAccountId")),
                stringValue(payload.get("categoryId")),
                LocalDate.parse(required(payload, "date")),
                stringValue(payload.get("note"))
        );
    }

    private LocalDate parseDateOrNull(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value);
    }

    private String required(Map<String, Object> payload, String key) {
        String value = stringValue(payload.get(key));
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("缺少字段：" + key);
        }
        return value;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private record TransactionDraft(
            TransactionType type,
            BigDecimal amount,
            String accountId,
            String targetAccountId,
            String categoryId,
            LocalDate date,
            String note
    ) {
    }
}

package com.familyledger.model;

import java.math.BigDecimal;

public final class Account {
    private final String id;
    private final String name;
    private final String type;
    private final BigDecimal openingBalance;

    public Account(String id, String name, String type, BigDecimal openingBalance) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.openingBalance = openingBalance;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String type() {
        return type;
    }

    public BigDecimal openingBalance() {
        return openingBalance;
    }
}

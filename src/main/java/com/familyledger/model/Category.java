package com.familyledger.model;

public final class Category {
    private final String id;
    private final String name;
    private final TransactionType type;

    public Category(String id, String name, TransactionType type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public TransactionType type() {
        return type;
    }
}

package com.twogathertales.encounterservice.util;

public enum Token {

    INSTANCE("Initial class info");

    private String value;

    private Token(String value) {
        this.value = value;
    }

    public Token getInstance() {
        return INSTANCE;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    // getters and setters
}
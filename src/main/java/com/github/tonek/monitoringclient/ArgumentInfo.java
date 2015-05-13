package com.github.tonek.monitoringclient;

import checkers.nullness.quals.NonNull;

class ArgumentInfo {
    @NonNull
    private final String name;

    public ArgumentInfo(@NonNull String name) {
        this.name = name;
    }

    @NonNull
    public String getName() {
        return name;
    }
}

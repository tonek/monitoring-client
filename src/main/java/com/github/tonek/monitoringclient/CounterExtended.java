package com.github.tonek.monitoringclient;

import checkers.nullness.quals.NonNull;
import com.codahale.metrics.Counter;

class CounterExtended extends Counter {
    @NonNull
    private final FullMetricKey metricKey;

    public CounterExtended(@NonNull FullMetricKey metricKey) {
        this.metricKey = metricKey;
    }

    @NonNull
    public FullMetricKey getMetricKey() {
        return metricKey;
    }

    @Override
    public String toString() {
        return "CounterExtended{metricKey=" + metricKey + '}';
    }
}

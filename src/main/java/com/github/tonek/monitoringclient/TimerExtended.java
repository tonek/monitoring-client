package com.github.tonek.monitoringclient;

import checkers.nullness.quals.NonNull;
import com.codahale.metrics.Timer;

class TimerExtended extends Timer {
    @NonNull
    private final FullMetricKey metricKey;

    public TimerExtended(@NonNull FullMetricKey metricKey) {
        this.metricKey = metricKey;
    }

    @NonNull
    public FullMetricKey getMetricKey() {
        return metricKey;
    }
}

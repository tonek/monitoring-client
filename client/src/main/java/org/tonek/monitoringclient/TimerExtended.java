package org.tonek.monitoringclient;

import com.codahale.metrics.Timer;

class TimerExtended extends Timer {
    private final FullMetricKey metricKey;

    public TimerExtended(FullMetricKey metricKey) {
        this.metricKey = metricKey;
    }

    public FullMetricKey getMetricKey() {
        return metricKey;
    }
}

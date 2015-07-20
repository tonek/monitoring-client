package org.tonek.monitoringclient;

import com.codahale.metrics.Counter;

class CounterExtended extends Counter {

    private final FullMetricKey metricKey;

    public CounterExtended(FullMetricKey metricKey) {
        this.metricKey = metricKey;
    }

    public FullMetricKey getMetricKey() {
        return metricKey;
    }

    @Override
    public String toString() {
        return "CounterExtended{metricKey=" + metricKey + '}';
    }
}

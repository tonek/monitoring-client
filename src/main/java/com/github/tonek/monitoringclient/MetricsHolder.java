package com.github.tonek.monitoringclient;

import com.codahale.metrics.*;
import com.codahale.metrics.Metric;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class MetricsHolder {
    private final ConcurrentHashMap<FullMetricKey, com.codahale.metrics.Metric> allMetrics
            = new ConcurrentHashMap<FullMetricKey, com.codahale.metrics.Metric>();

    private final MetricRegistry metricRegistry;

    public MetricsHolder(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    public Object getMetric(
            Class<? extends com.codahale.metrics.Metric> metricType,
            String metricsGroupName,
            String methodName,
            Map<String, Object> arguments) {

        FullMetricKey key = new FullMetricKey(metricsGroupName, methodName, arguments);
        com.codahale.metrics.Metric metric = allMetrics.get(key);
        if (metric == null) {
            metric = createMetric(metricType, key);
            com.codahale.metrics.Metric previousValue = allMetrics.putIfAbsent(key, metric);
            if (previousValue != null) {
                metric = previousValue;
            } else {
                metricRegistry.register(key.toIdString(), metric);
            }
        }
        return metric;
    }

    private Metric createMetric(Class<?> metricType, FullMetricKey key) {
        if (metricType.equals(Counter.class)) {
            return new CounterExtended(key);
        } else if (metricType.equals(Timer.class)) {
            return new TimerExtended(key);
        } else {
            throw new IllegalArgumentException("Unexpected metricType: " + metricType);
        }
    }
}

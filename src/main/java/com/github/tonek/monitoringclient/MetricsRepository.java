package com.github.tonek.monitoringclient;


import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MetricsRepository {
    private final Map<Class<?>, Object> metricsProxies;

    public MetricsRepository(MetricsWrapperFactory factory, Class<?>... classes) {
        for (Class<?> clazz : classes) {
            assertValidMetricGroupClass(clazz);
        }
        Map<Class<?>, Object> proxies = new HashMap<Class<?>, Object>();
        MetricRegistry metricRegistry = new MetricRegistry();
        MetricsHolder metricsHolder = new MetricsHolder(metricRegistry);
        for (Class<?> clazz : classes) {
            Object proxy = factory.createWrapper(metricsHolder, clazz);
            proxies.put(clazz, proxy);
        }

        RestReporter reporter = new RestReporter(
                metricRegistry, new ObjectMapper(), new RestReporter.Config("test_pr", "localhost", 8080));
        reporter.start(500, TimeUnit.MILLISECONDS);

        metricsProxies = Collections.unmodifiableMap(proxies);

    }

    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> metricsGroupClass) {
        return (T) metricsProxies.get(metricsGroupClass);
    }

    private static void assertValidMetricGroupClass(Class<?> clazz) {
        //TODO: assert valid annotations present.
    }
}

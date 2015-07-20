package org.tonek.monitoringclient;

public interface MetricsWrapperFactory {
    <T> T createWrapper(MetricsHolder metricsHolder, Class<T> clazz);
}

package org.tonek.monitoringclient;

import com.codahale.metrics.Counter;

public interface BenchmarkMetrics {
    @Metric(description = "How many sheep we saw")
    Counter sheepSeen(@MetricArgument("color") String color, @MetricArgument("age") int age);
}

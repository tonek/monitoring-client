package org.tonek.monitoringclient;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;

@MetricsGroup("Sheep metrics")
public interface SheepMetrics {

    @Metric(description = "How many sheep we saw")
    Counter sheepSeen(@MetricArgument("color") String color, @MetricArgument("age") int age);
    Counter sheepSeen(@MetricArgument("age") int age);
    Counter sheepSeen(@MetricArgument("color") String color);
    Counter sheepSeen();

    @Metric
    Timer sheepJumpDuration(@MetricArgument("ownerName") String ownerName);
}

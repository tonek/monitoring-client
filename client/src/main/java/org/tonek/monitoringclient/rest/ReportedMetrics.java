package org.tonek.monitoringclient.rest;

import java.util.List;

public class ReportedMetrics {
    private final List<ReportedMetric> metrics;

    public ReportedMetrics(List<ReportedMetric> metrics) {
        this.metrics = metrics;
    }

    public List<ReportedMetric> getMetrics() {
        return metrics;
    }

    @Override
    public String toString() {
        return "ReportedMetrics{" +
                "metrics=" + metrics +
                '}';
    }
}

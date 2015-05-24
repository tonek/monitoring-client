package com.github.tonek.monitoringclient;

import java.util.Collections;
import java.util.List;

class MethodInfo {
    private final Class<?> metricType;
    private final String metricName;
    private final String metricGroupName;
    private final List<ArgumentInfo> argumentInfos;

    public MethodInfo(Class<?> metricType, String metricName, String metricGroupName,
                      List<ArgumentInfo> argumentInfos) {
        this.metricType = metricType;
        this.metricName = metricName;
        this.metricGroupName = metricGroupName;
        this.argumentInfos = argumentInfos == null
                ? Collections.<ArgumentInfo>emptyList() : Collections.unmodifiableList(argumentInfos);
    }

    public Class<?> getMetricType() {
        return metricType;
    }

    public String getMetricName() {
        return metricName;
    }

    public String getMetricGroupName() {
        return metricGroupName;
    }

    public List<ArgumentInfo> getArgumentInfos() {
        return argumentInfos;
    }
}

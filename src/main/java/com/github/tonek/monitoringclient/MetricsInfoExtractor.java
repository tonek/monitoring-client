package com.github.tonek.monitoringclient;

import org.springframework.core.LocalVariableTableParameterNameDiscoverer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

class MetricsInfoExtractor {
    MethodInfo getMethodInfo(Method method, String metricGroupName) {
        String metricName = method.getName();
        com.github.tonek.monitoringclient.Metric metricAnnotation
                = method.getAnnotation(com.github.tonek.monitoringclient.Metric.class);
        if (metricAnnotation != null && metricAnnotation.id() != null && !metricAnnotation.id().trim().isEmpty()) {
            metricName = metricAnnotation.id();
        }
        String[] parameterNames = new LocalVariableTableParameterNameDiscoverer().getParameterNames(method);
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        List<ArgumentInfo> arguments = new ArrayList<ArgumentInfo>(parameterAnnotations.length);
        for (int i = 0; i < parameterAnnotations.length; i++) {
            String argumentName = parameterNames == null ? null : parameterNames[i];
            Annotation[] annotations = parameterAnnotations[i];
            if (annotations != null) {
                for (Annotation annotation : annotations) {
                    if (annotation.annotationType() == MetricArgument.class) {
                        MetricArgument metricsArg = (MetricArgument) annotation;
                        if (metricsArg.value() != null && !metricsArg.value().trim().isEmpty()) {
                            argumentName = metricsArg.value();
                        }
                    }
                }
            }
            arguments.add(new ArgumentInfo(argumentName));
        }
        return new MethodInfo(method.getReturnType(), metricName, metricGroupName, arguments);
    }

    String getMetricsGroupName(Class<?> metricsGroupClass) {
        MetricsGroup metricsGroup = metricsGroupClass.getAnnotation(MetricsGroup.class);
        if (metricsGroup == null || metricsGroup.value() == null || metricsGroup.value().trim().isEmpty()) {
            return metricsGroupClass.getSimpleName();
        } else {
            return metricsGroup.value();
        }
    }
}

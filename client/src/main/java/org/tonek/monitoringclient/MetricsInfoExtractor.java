package org.tonek.monitoringclient;

import org.springframework.core.LocalVariableTableParameterNameDiscoverer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class MetricsInfoExtractor {
    MethodInfo getMethodInfo(Method method, String metricGroupName) {
        String metricName = method.getName();
        Metric metricAnnotation = method.getAnnotation(Metric.class);
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

    Map<String, Object> getParametersMaps(Object[] args, MethodInfo methodInfo) {
        List<ArgumentInfo> argumentInfos = methodInfo.getArgumentInfos();
        Map<String, Object> parametersMap = null;
        for (int i = 0; i < argumentInfos.size(); i++) {
            Object arg = args[i];
            if (arg != null) {
                if (parametersMap == null) {
                    parametersMap = new HashMap<String, Object>();
                }
                parametersMap.put(argumentInfos.get(i).getName(), arg);
            }
        }
        return parametersMap;
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

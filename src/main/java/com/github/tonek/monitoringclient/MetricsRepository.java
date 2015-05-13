package com.github.tonek.monitoringclient;


import com.codahale.metrics.*;
import com.codahale.metrics.Metric;
import com.codahale.metrics.Timer;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class MetricsRepository {
    private final ConcurrentHashMap<FullMetricKey, com.codahale.metrics.Metric> allMetrics
            = new ConcurrentHashMap<FullMetricKey, Metric>();

    private final ConcurrentHashMap<Method, MethodInfo> methodInfos = new ConcurrentHashMap<Method, MethodInfo>();

    private final Map<Class<?>, Object> metricsProxies;

    private final MetricRegistry metricRegistry;

    public MetricsRepository(Class<?>... classes) {
        for (Class<?> clazz : classes) {
            assertValidMetricGroupClass(clazz);
        }
        Map<Class<?>, Object> proxies = new HashMap<Class<?>, Object>();
        metricRegistry = new MetricRegistry();
        for (Class<?> clazz : classes) {
            MetricsInvocationHandler handler = new MetricsInvocationHandler(clazz);
            Object proxy = Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, handler);
            proxies.put(clazz, proxy);
        }

        InfluxDbReporter reporter = new InfluxDbReporter(metricRegistry);
        reporter.start(10, TimeUnit.MILLISECONDS);

        metricsProxies = Collections.unmodifiableMap(proxies);

    }

    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> metricsGroupClass) {
        return (T) metricsProxies.get(metricsGroupClass);
    }

    private void initMetricMethods(Class<?> metricsGroupClass) {

    }

    private Object getMetric(
            Class<? extends com.codahale.metrics.Metric> metricType,
            String metricsGroupName,
            String methodName,
            Map<String, Object> arguments) {

        FullMetricKey key = new FullMetricKey(metricsGroupName, methodName, arguments);
        Metric metric = allMetrics.get(key);
        if (metric == null) {
            metric = createMetric(metricType, key);
            Metric previousValue = allMetrics.putIfAbsent(key, metric);
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

    private static void assertValidMetricGroupClass(Class<?> clazz) {

    }

    private String getMetricsGroupName(Class<?> metricsGroupClass) {
        String metricsGroupName;MetricsGroup metricsGroup = metricsGroupClass.getAnnotation(MetricsGroup.class);
        if (metricsGroup == null || metricsGroup.value() == null || metricsGroup.value().trim().isEmpty()) {
            metricsGroupName = metricsGroupClass.getSimpleName();
        } else {
            metricsGroupName = metricsGroup.value();
        }
        return metricsGroupName;
    }

    private class MetricsInvocationHandler implements InvocationHandler {
        private final String metricsGroupName;

        public MetricsInvocationHandler(Class<?> metricsGroupClass) {
            metricsGroupName = getMetricsGroupName(metricsGroupClass);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            MethodInfo methodInfo = getMethodInfo(method, metricsGroupName);
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

            String methodName = method.getName();
            @SuppressWarnings("unchecked")
            Class<? extends com.codahale.metrics.Metric> metricType
                    = (Class<? extends com.codahale.metrics.Metric>) method.getReturnType();

            return getMetric(metricType, metricsGroupName, methodName, parametersMap);
        }
    }

    @SuppressWarnings("unchecked")
    private MethodInfo getMethodInfo(Method method, String metricGroupName) {
        MethodInfo methodInfo = methodInfos.get(method);
        if (methodInfo != null) {
            return methodInfo;
        }
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
        methodInfo = new MethodInfo(method.getReturnType(), metricName, metricGroupName, arguments);
        methodInfos.put(method, methodInfo);
        return methodInfo;
    }

}

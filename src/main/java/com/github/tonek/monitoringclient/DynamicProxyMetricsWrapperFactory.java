package com.github.tonek.monitoringclient;

import org.springframework.core.LocalVariableTableParameterNameDiscoverer;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DynamicProxyMetricsWrapperFactory implements MetricsWrapperFactory {

    @SuppressWarnings("unchecked")
    @Override
    public <T> T createWrapper(MetricsHolder metricsHolder, Class<T> clazz) {
        MetricsInvocationHandler handler = new MetricsInvocationHandler(clazz, metricsHolder);
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, handler);
    }

    private static class MetricsInvocationHandler implements InvocationHandler {
        private final String metricsGroupName;
        private final MetricsHolder metricsHolder;
        private final ConcurrentHashMap<Method, MethodInfo> methodInfos;

        public MetricsInvocationHandler(Class<?> metricsGroupClass, MetricsHolder metricsHolder) {
            this.metricsHolder = metricsHolder;
            methodInfos = new ConcurrentHashMap<Method, MethodInfo>();
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

            return metricsHolder.getMetric(metricType, metricsGroupName, methodName, parametersMap);
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
}

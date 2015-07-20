package org.tonek.monitoringclient;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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
        private final MetricsInfoExtractor metricsInfoExtractor;

        public MetricsInvocationHandler(Class<?> metricsGroupClass, MetricsHolder metricsHolder) {
            this.metricsHolder = metricsHolder;
            methodInfos = new ConcurrentHashMap<Method, MethodInfo>();
            metricsInfoExtractor = new MetricsInfoExtractor();
            metricsGroupName = metricsInfoExtractor.getMetricsGroupName(metricsGroupClass);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            MethodInfo methodInfo = getMethodInfo(method, metricsGroupName);
            Map<String, Object> parametersMap = metricsInfoExtractor.getParametersMaps(args, methodInfo);

            return metricsHolder.getMetric(methodInfo.getMetricType(),
                    methodInfo.getMetricGroupName(), methodInfo.getMetricName(), parametersMap);
        }

        @SuppressWarnings("unchecked")
        private MethodInfo getMethodInfo(Method method, String metricGroupName) {
            MethodInfo methodInfo = methodInfos.get(method);
            if (methodInfo != null) {
                return methodInfo;
            }
            methodInfo = metricsInfoExtractor.getMethodInfo(method, metricGroupName);
            methodInfos.put(method, methodInfo);
            return methodInfo;
        }
    }
}

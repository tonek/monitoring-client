package org.tonek.monitoringclient;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import java.lang.reflect.Method;
import net.bytebuddy.matcher.BooleanMatcher;

import java.util.HashMap;
import java.util.Map;

public class ByteCodeGeneratedMetricsWrapperFactory implements MetricsWrapperFactory {
    private final MetricsInfoExtractor metricsInfoExtractor;

    public ByteCodeGeneratedMetricsWrapperFactory(MetricsInfoExtractor metricsInfoExtractor) {
        this.metricsInfoExtractor = metricsInfoExtractor;
    }

    @Override
    public <T> T createWrapper(MetricsHolder metricsHolder, Class<T> clazz) {
        String metricsGroupName = metricsInfoExtractor.getMetricsGroupName(clazz);
        Method[] methods = clazz.getDeclaredMethods();
        Map<Method, MethodInfo> methodInfos = new HashMap<Method, MethodInfo>();
        for (Method method : methods) {
            methodInfos.put(method, metricsInfoExtractor.getMethodInfo(method, metricsGroupName));
        }
        GeneralInterceptor interceptor = new GeneralInterceptor(methodInfos, metricsInfoExtractor, metricsHolder);
        Class<? extends T> dynamicType = new ByteBuddy()
                .subclass(clazz)
                .method(new BooleanMatcher<MethodDescription>(true))
                .intercept(MethodDelegation.to(interceptor))
                .make().load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();

        try {
            return dynamicType.newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException("Cannot create metrics wrapper for " + clazz, e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot create metrics wrapper for " + clazz, e);
        }
    }

    public static class GeneralInterceptor {
        private final Map<Method, MethodInfo> methodInfos;
        private final MetricsInfoExtractor metricsInfoExtractor;
        private final MetricsHolder metricsHolder;

        public GeneralInterceptor(Map<Method, MethodInfo> methodInfos,
                                  MetricsInfoExtractor metricsInfoExtractor,
                                  MetricsHolder metricsHolder) {
            this.methodInfos = methodInfos;
            this.metricsInfoExtractor = metricsInfoExtractor;
            this.metricsHolder = metricsHolder;
        }

        @RuntimeType
        public Object intercept(@AllArguments Object[] allArguments,
                                @Origin Method method) {
            MethodInfo methodInfo = methodInfos.get(method);
            Map<String, Object> parametersMaps = metricsInfoExtractor.getParametersMaps(allArguments, methodInfo);

            return metricsHolder.getMetric(methodInfo.getMetricType(),
                    methodInfo.getMetricGroupName(), methodInfo.getMetricName(), parametersMaps);
        }
    }
}

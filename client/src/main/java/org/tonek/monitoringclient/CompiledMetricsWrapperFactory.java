package org.tonek.monitoringclient;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class CompiledMetricsWrapperFactory implements MetricsWrapperFactory {
    private final MetricsInfoExtractor metricsInfoExtractor;

    public CompiledMetricsWrapperFactory(MetricsInfoExtractor metricsInfoExtractor) {
        this.metricsInfoExtractor = metricsInfoExtractor;
    }

    @Override
    public <T> T createWrapper(MetricsHolder metricsHolder, Class<T> clazz) {
        String groupName = metricsInfoExtractor.getMetricsGroupName(clazz);
        StringBuilder classBody = new StringBuilder();
        String className = declareImplementation(classBody, clazz);
        Method[] methods = clazz.getDeclaredMethods();
        MethodInfo[] methodInfos = new MethodInfo[methods.length];
        classBody.append(" {\n");
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            MethodInfo methodInfo = metricsInfoExtractor.getMethodInfo(method, groupName);
            methodInfos[i] = methodInfo;
            implementMetricsMethod(classBody, method, methodInfo, i);
        }
        classBody.append("\n}");
        try {
            MetricsCompiledImplementation compileMetrics = compileMetrics(classBody.toString(), className);
            compileMetrics.setProcessor(new Processor(metricsHolder, methodInfos));
            @SuppressWarnings("unchecked")
            T result = (T) compileMetrics;
            return result;
        } catch (IOException e) {
            throw new RuntimeException("Cannot compile class " + className, e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Cannot find class " + className, e);
        } catch (InstantiationException e) {
            throw new RuntimeException("Cannot instantiate class " + className, e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot access class " + className, e);
        }
    }

    private MetricsCompiledImplementation compileMetrics(String classBody, String className)
            throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {

        File tempFile = File.createTempFile("test", "metrics");
        File tempDirectory = tempFile.getParentFile();
        File root = new File(tempDirectory, "gen_metrics" + System.nanoTime());
        root.mkdir();
        File sourceFile = new File(root, "org/tonek/monitoringclient/" + className + ".java");
        sourceFile.getParentFile().mkdirs();
        FileOutputStream fileOutputStream = new FileOutputStream(sourceFile);
        fileOutputStream.write(classBody.getBytes(StandardCharsets.UTF_8));
        fileOutputStream.close();

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        compiler.run(null, null, null, sourceFile.getPath());

        URLClassLoader classLoader = URLClassLoader.newInstance(
                new URL[]{root.toURI().toURL()}, this.getClass().getClassLoader());
        Class<?> cls = Class.forName("org.tonek.monitoringclient." + className, true, classLoader);
        return (MetricsCompiledImplementation) cls.newInstance();
    }

    private String declareImplementation(StringBuilder classBody, Class<?> clazz) {
        String className = clazz.getSimpleName() + "$$_Metrics_Impl";
        classBody
                .append("package org.tonek.monitoringclient;\n")
                .append("public class ").append(className)
                .append(" extends ").append(MetricsCompiledImplementation.class.getCanonicalName())
                .append("\n\timplements ").append(clazz.getCanonicalName());
        return className;
    }

    private void implementMetricsMethod(StringBuilder classBody, Method method, MethodInfo methodInfo, int index) {
        String returnTypeString = methodInfo.getMetricType().getCanonicalName();
        classBody.append("\tpublic ").append(returnTypeString).append(' ').append(method.getName()).append('(');
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            if (i > 0) {
                classBody.append(',');
            }
            classBody.append(parameter.getType().getCanonicalName()).append(' ').append("arg").append(i);
        }
        classBody.append(") {\n")
                .append("\t\tObject[] allArgs = new Object[").append(parameters.length).append("];\n");

        for(int i = 0; i < parameters.length; i++) {
            classBody.append("\t\tallArgs[").append(i).append("] = arg").append(i).append(";\n");
        }
        classBody.append("\t\tObject result = getProcessor().process(allArgs, ").append(index).append(");\n")
                .append("\t\treturn (").append(returnTypeString).append(") result;\n\t}\n\n");
    }

    public abstract static class MetricsCompiledImplementation {
        protected Processor processor;

        void setProcessor(Processor processor) {
            this.processor = processor;
        }

        public Processor getProcessor() {
            return processor;
        }
    }

    public class Processor {
        private final MetricsHolder metricsHolder;
        private final MethodInfo[] methodInfos;

        public Processor(MetricsHolder metricsHolder, MethodInfo[] methodInfos) {
            this.metricsHolder = metricsHolder;
            this.methodInfos = methodInfos;
        }

        public Object process(Object allArgs[], int index) {
            MethodInfo methodInfo = methodInfos[index];
            Map<String, Object> parametersMaps = metricsInfoExtractor.getParametersMaps(allArgs, methodInfo);
            return metricsHolder.getMetric(methodInfo.getMetricType(), methodInfo.getMetricGroupName(),
                    methodInfo.getMetricName(), parametersMaps);
        }
    }
}

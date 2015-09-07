package org.tonek.monitoringclient;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.matcher.BooleanMatcher;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@Warmup(time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(time = 500, timeUnit = TimeUnit.MILLISECONDS)
@CompilerControl(CompilerControl.Mode.DONT_INLINE)
public class InterfaceImplementationBenchmark {
    private TestInterface intercepted;
    private TestInterface implemented;
    private TestInterface dynamic;
    private TestInterface compiled;

    private Integer diff;

    @Setup
    public void setup() throws IllegalAccessException, InstantiationException, IOException, ClassNotFoundException {

        Interceptor interceptor = new Interceptor();
        intercepted = new ByteBuddy()
                .subclass(TestInterface.class)
                .method(new BooleanMatcher<MethodDescription>(true))
                .intercept(MethodDelegation.to(interceptor))
                .make()
                .load(MetricsRepository.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .newInstance();

        implemented = new TestInterface() {
            private int value;
            @Override
            public int test(Integer val) {
                value += val;
                return value;
            }
        };

        dynamic = (TestInterface) Proxy.newProxyInstance(TestInterface.class.getClassLoader(), new Class[]{TestInterface.class},
                new InvocationHandler() {
                    private int value;
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        value += ((Integer)args[0]);
                        return value;
                    }
                });

        String source = "package org.tonek.monitoringclient; public class TestComp implements TestInterface { private int value;\n" +
                "            @Override\n" +
                "            public int test(Integer val) {\n" +
                "                value += val;\n" +
                "                return value;\n" +
                "            } }";

        compiled = createCompiled(source);

        diff = 1;

    }

    private TestInterface createCompiled(String source) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        File root = new File("~/java"); // On Windows running on C:\, this is C:\java.
        root.mkdirs();
        System.out.println(root.getAbsoluteFile());
        File sourceFile = new File(root, "org/tonek/monitoringclient/TestComp.java");
        sourceFile.getParentFile().mkdirs();
        FileOutputStream fileOutputStream = new FileOutputStream(sourceFile);
        fileOutputStream.write(source.getBytes(StandardCharsets.UTF_8));
        fileOutputStream.close();

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        compiler.run(null, null, null, sourceFile.getPath());

        URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{root.toURI().toURL()});
        Class<?> cls = Class.forName("org.tonek.monitoringclient.TestComp", true, classLoader);
        return (TestInterface) cls.newInstance();
    }

    public static class Interceptor {
        private int value;
        @RuntimeType
        public Object intercept(@AllArguments Object[] allArguments,
                                @Origin Method method) {
            value += ((Integer)allArguments[0]);
            return value;
        }
    }

    @Benchmark
    public int intercepted() {
        return intercepted.test(diff);
    }

    @Benchmark
    public int implemented() {
        return implemented.test(diff);
    }

    @Benchmark
    public int dynamic() {
        return dynamic.test(diff);
    }

    @Benchmark
    public int compiled() {
        return compiled.test(diff);
    }
}

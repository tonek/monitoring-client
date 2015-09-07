package org.tonek.monitoringclient;

import com.codahale.metrics.Counter;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@Fork(3)
@Warmup(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@CompilerControl(CompilerControl.Mode.DONT_INLINE)
public class ProxyTypesBenchmark {
    private BenchmarkMetrics dynamic;
    private BenchmarkMetrics generated;
    private BenchmarkMetrics compiled;

    @Setup
    public void setup() {
        MetricsRepository dynamicRepository = new MetricsRepository(
                new DynamicProxyMetricsWrapperFactory(), BenchmarkMetrics.class);
        dynamic = dynamicRepository.get(BenchmarkMetrics.class);

        MetricsRepository generatedRepository = new MetricsRepository(
                new ByteCodeGeneratedMetricsWrapperFactory(new MetricsInfoExtractor()), BenchmarkMetrics.class);
        generated = generatedRepository.get(BenchmarkMetrics.class);

        MetricsRepository compiledRepository = new MetricsRepository(
                new CompiledMetricsWrapperFactory(new MetricsInfoExtractor()), BenchmarkMetrics.class);
        compiled = compiledRepository.get(BenchmarkMetrics.class);
    }

    @Benchmark
    public Counter dynamic() {
        return dynamic.sheepSeen("black", 2);
    }

    @Benchmark
    public Counter generated() {
        return generated.sheepSeen("black", 2);
    }

    @Benchmark
    public Counter compiled() {
        return compiled.sheepSeen("black", 2);
    }
}

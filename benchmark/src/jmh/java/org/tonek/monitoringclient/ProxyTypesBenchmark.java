package org.tonek.monitoringclient;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@Warmup(time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(time = 500, timeUnit = TimeUnit.MILLISECONDS)
public class ProxyTypesBenchmark {
    private BenchmarkMetrics dynamic;
    private BenchmarkMetrics generated;

    @Setup
    public void setup() {
        MetricsRepository dynamicRepository = new MetricsRepository(
                new DynamicProxyMetricsWrapperFactory(), BenchmarkMetrics.class);
        dynamic = dynamicRepository.get(BenchmarkMetrics.class);

        MetricsRepository generatedRepository = new MetricsRepository(
                new GeneratedMetricsWrapperFactory(new MetricsInfoExtractor()), BenchmarkMetrics.class);
        generated = generatedRepository.get(BenchmarkMetrics.class);
    }

    @Benchmark
    public void dynamic() {
        dynamic.sheepSeen("black", 2);
    }

    @Benchmark
    public void generated() {
        generated.sheepSeen("black", 2);
    }
}

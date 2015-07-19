package com.github.tonek.monitoringclient;

import com.codahale.metrics.Timer;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;


public class TestMetrics {
    private SheepMetrics sheepMetrics;

    @Before
    public void setUp() {
        MetricsWrapperFactory factory = new DynamicProxyMetricsWrapperFactory();
        MetricsRepository repository = new MetricsRepository(factory, SheepMetrics.class);
        sheepMetrics = repository.get(SheepMetrics.class);
    }

    @Test
    public void testSameCounterReturnedForSameArguments() {
        String color = "white";
        int age = 2;

        assertThat(sheepMetrics.sheepSeen(color, age)).isSameAs(sheepMetrics.sheepSeen(color, age));
    }

    @Test
    public void testSameCounterReturnedWhenArgumentsMissingAndNull() throws InterruptedException {
        assertThat(sheepMetrics.sheepSeen(null)).isSameAs(sheepMetrics.sheepSeen());
        assertThat(sheepMetrics.sheepSeen(null, 2)).isSameAs(sheepMetrics.sheepSeen(2));
    }

    @Test
    public void testMetricsReported() throws InterruptedException {
        Random random = new Random();
        while(true) {
            sheepMetrics.sheepSeen(null).inc(10);
            sheepMetrics.sheepSeen(2).inc(1);
            sheepMetrics.sheepSeen("white", 5).inc(3);

            Thread.sleep(random.nextInt(50));

            sheepMetrics.sheepSeen("white", 5).inc(1);
            sheepMetrics.sheepSeen("black", 3).inc(2);
            Timer.Context context = sheepMetrics.sheepJumpDuration("Anton").time();

            Thread.sleep(random.nextInt(50));
            context.stop();

            Thread.sleep(random.nextInt(500));
        }

    }
}

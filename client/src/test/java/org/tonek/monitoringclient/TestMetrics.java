package org.tonek.monitoringclient;

import com.codahale.metrics.Timer;
import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;


@RunWith(Parameterized.class)
public class TestMetrics {
    @Parameterized.Parameters
    public static Collection<MetricsWrapperFactory> data() {
        return Arrays.asList(
                new CompiledMetricsWrapperFactory(new MetricsInfoExtractor()),
                new DynamicProxyMetricsWrapperFactory(),
                new ByteCodeGeneratedMetricsWrapperFactory(new MetricsInfoExtractor())
        );
    }

    private MetricsWrapperFactory metricsWrapperFactory;
    private SheepMetrics sheepMetrics;

    public TestMetrics(MetricsWrapperFactory metricsWrapperFactory) {
        this.metricsWrapperFactory = metricsWrapperFactory;
    }

    @Before
    public void setUp() {
        MetricsRepository repository = new MetricsRepository(metricsWrapperFactory, SheepMetrics.class);
        sheepMetrics = repository.get(SheepMetrics.class);
    }

    @Test
    public void testSameCounterReturnedForSameArguments() {
        String color = "white";
        int age = 2;

        assertThat(sheepMetrics.sheepSeen(color, age)).isNotNull().isSameAs(sheepMetrics.sheepSeen(color, age));
    }

    @Test
    public void testSameCounterReturnedWhenArgumentsMissingAndNull() throws InterruptedException {
        assertThat(sheepMetrics.sheepSeen(null)).isNotNull().isSameAs(sheepMetrics.sheepSeen());
        assertThat(sheepMetrics.sheepSeen(null, 2)).isNotNull().isSameAs(sheepMetrics.sheepSeen(2));
    }

    @Test
    public void testMetricsReported() throws InterruptedException {
        Random random = new Random();
        int cnt = 10;
        while(cnt > 0) {
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
            cnt--;
        }

    }
}

package com.github.tonek.monitoringclient;

import checkers.nullness.quals.NonNull;
import checkers.nullness.quals.Nullable;
import com.codahale.metrics.*;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Serie;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

class InfluxDbReporter extends ScheduledReporter {
    private final InfluxDB influxDB = InfluxDBFactory.connect("http://192.168.0.104:8086", "reporter", "qwerty");
    private final ConcurrentHashMap<FullMetricKey, Long> previousValues
            = new ConcurrentHashMap<FullMetricKey, Long>();

    InfluxDbReporter(MetricRegistry registry) {
        super(registry, "influxTest", MetricFilter.ALL, TimeUnit.SECONDS, TimeUnit.MILLISECONDS);
    }

    @Override
    public void report(
            SortedMap<String, Gauge> gauges,
            SortedMap<String, Counter> counters,
            SortedMap<String, Histogram> histograms,
            SortedMap<String, Meter> meters,
            SortedMap<String, Timer> timers) {


        try {
            List<Serie> series = null;
            for (Counter counter : counters.values()) {
                if (counter instanceof CounterExtended) {
                    List<Serie> counterSeries = toSeries((CounterExtended) counter);
                    if (!counterSeries.isEmpty()) {
                        if (series == null) {
                            series = new ArrayList<Serie>();
                        }
                        series.addAll(counterSeries);
                    }
                }
            }
            if (series != null) {
                influxDB.write("test", TimeUnit.MILLISECONDS, series.toArray(new Serie[series.size()]));
                System.out.println("Reported: " + series);
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        }

    }

    @Nullable
    private List<Serie> toSeries(CounterExtended counter) {
        FullMetricKey key = counter.getMetricKey();
        long diff = 0;
        while(true) {
            Long oldValue = previousValues.get(key);
            if (oldValue != null && oldValue == counter.getCount()) {
                break;
            } else {
                if (oldValue == null) {
                    if (previousValues.putIfAbsent(key, counter.getCount()) == null) {
                        diff = counter.getCount();
                        break;
                    }
                } else if (previousValues.replace(key, oldValue, counter.getCount())) {
                    diff = counter.getCount() - oldValue;
                    break;
                }
            }
        }

        if (diff <= 0) {
            return Collections.emptyList();
        }

        List<Serie> result = new ArrayList<Serie>((int)diff);
        Serie serie = toSerie(key);
        for (int i = 0; i < diff; i++) {
            result.add(serie);
        }

        return result;
    }

    @NonNull
    private Serie toSerie(FullMetricKey key) {
        Serie.Builder builder = new Serie.Builder(key.toMetricId());
        ArrayList<String> keysList = new ArrayList<String>(key.getArguments().keySet());
        keysList.add("v");
        builder.columns(keysList.toArray(new String[keysList.size()]));

        Object[] values = new Object[keysList.size()];

        for (int i = 0; i < keysList.size() - 1; i++) {
            values[i] = key.getArguments().get(keysList.get(i));
        }
        values[values.length - 1] = 1;
        builder.values(values);
        return builder.build();
    }
}

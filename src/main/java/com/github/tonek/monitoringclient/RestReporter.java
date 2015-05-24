package com.github.tonek.monitoringclient;

import com.codahale.metrics.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tonek.monitoringclient.rest.ReportedMetric;
import com.github.tonek.monitoringclient.rest.ReportedMetrics;
import org.apache.http.HttpHeaders;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class RestReporter extends ScheduledReporter {
    private static final Logger log = LoggerFactory.getLogger(RestReporter.class);

    private final ConcurrentHashMap<FullMetricKey, Long> previousValues
            = new ConcurrentHashMap<FullMetricKey, Long>();

    private final ObjectMapper objectMapper;

    private final Config config;

    RestReporter(MetricRegistry registry, ObjectMapper objectMapper, Config config) {
        super(registry, "restReporter", MetricFilter.ALL, TimeUnit.SECONDS, TimeUnit.MILLISECONDS);
        this.objectMapper = objectMapper;
        this.config = config;

        log.debug("ResReporter is created with config {}.", config);
    }

    @Override
    public void report(
            SortedMap<String, Gauge> gauges,
            SortedMap<String, Counter> counters,
            SortedMap<String, Histogram> histograms,
            SortedMap<String, Meter> meters,
            SortedMap<String, Timer> timers) {
        List<ReportedMetric> metricsToReport = null;
        for (Counter counter : counters.values()) {
            if (counter instanceof CounterExtended) {
                ReportedMetric metric = toReportedMetric((CounterExtended) counter);
                if (metric != null) {
                    if (metricsToReport == null) {
                        metricsToReport = new ArrayList<ReportedMetric>();
                    }
                    metricsToReport.add(metric);
                }
            }
        }
        if (metricsToReport != null && !metricsToReport.isEmpty()) {
            sendMetrics(metricsToReport);

        } else {
            log.debug("No new metrics to report");
        }
    }

    private ReportedMetric toReportedMetric(CounterExtended counter) {
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
            return null;
        }

        List<ReportedMetric.Argument> arguments = null;
        Map<String, Object> keyArguments = key.getArguments();
        if (!keyArguments.isEmpty()) {
            arguments = new ArrayList<ReportedMetric.Argument>(keyArguments.size());
            for (Map.Entry<String, Object> entry : keyArguments.entrySet()) {
                arguments.add(new ReportedMetric.Argument(entry.getKey(), entry.getValue().toString()));
            }
        }
        return new ReportedMetric(key.getGroupName(), key.getName(), arguments, (int) diff);
    }

    private void sendMetrics(List<ReportedMetric> metrics) {
        ReportedMetrics request = new ReportedMetrics(metrics);
        try {
            String json = objectMapper.writeValueAsString(request);
            String url = String.format("http://%s:%s/%s/reporting/v1/statistics",
                    config.getReportingHost(), config.getReportingPort(), config.getProjectId());

            log.debug("Sending metrics {} to {}.", request, url);

            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader(new BasicHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType()));
            httpPost.setEntity(new StringEntity(json));

            CloseableHttpClient client = HttpClients.createDefault();
            CloseableHttpResponse response = client.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode < 200 || statusCode >= 300) {
                throw new MonitoringException("Request to reporting server failed: " + response.getStatusLine());
            }

            log.debug("Metrics successfully reported to {}.", url);

        } catch (JsonProcessingException e) {
            throw new MonitoringException("Cannot report metrics: " + metrics.size(), e);
        } catch (ClientProtocolException e) {
            throw new MonitoringException("Cannot report metrics: " + metrics.size(), e);
        } catch (UnsupportedEncodingException e) {
            throw new MonitoringException("Cannot report metrics: " + metrics.size(), e);
        } catch (IOException e) {
            throw new MonitoringException("Cannot report metrics: " + metrics.size(), e);
        }

    }

    public static class Config {
        private final String projectId;
        private final String reportingHost;
        private final int reportingPort;

        public Config(String projectId, String reportingHost, int reportingPort) {
            this.projectId = projectId;
            this.reportingHost = reportingHost;
            this.reportingPort = reportingPort;
        }

        public String getProjectId() {
            return projectId;
        }

        public String getReportingHost() {
            return reportingHost;
        }

        public int getReportingPort() {
            return reportingPort;
        }

        @Override
        public String toString() {
            return "Config{" +
                    "projectId='" + projectId + '\'' +
                    ", reportingHost='" + reportingHost + '\'' +
                    ", reportingPort=" + reportingPort +
                    '}';
        }
    }
}

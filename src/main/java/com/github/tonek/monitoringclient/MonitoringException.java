package com.github.tonek.monitoringclient;

public class MonitoringException extends RuntimeException {
    public MonitoringException(String message) {
        super(message);
    }

    public MonitoringException(String message, Throwable cause) {
        super(message, cause);
    }

    public MonitoringException(Throwable cause) {
        super(cause);
    }

    public MonitoringException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

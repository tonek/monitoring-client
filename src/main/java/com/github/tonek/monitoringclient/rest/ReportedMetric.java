package com.github.tonek.monitoringclient.rest;

import java.util.Collections;
import java.util.List;

public class ReportedMetric {
    private String groupName;
    private String name;
    private List<Argument> arguments;
    private int count;

    public ReportedMetric(String groupName, String name, List<Argument> arguments, int count) {
        this.groupName = groupName;
        this.name = name;
        this.arguments = arguments;
        this.count = count;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Argument> getArguments() {
        return arguments == null ? Collections.<Argument>emptyList() : arguments;
    }

    public void setArguments(List<Argument> arguments) {
        this.arguments = arguments;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    @Override
    public String toString() {
        return "ReportedMetric{" +
                "groupName='" + groupName + '\'' +
                ", name='" + name + '\'' +
                ", arguments=" + arguments +
                '}';
    }

    public static class Argument {
        private String k;
        private String v;

        public Argument(String k, String v) {
            this.k = k;
            this.v = v;
        }

        public String getK() {
            return k;
        }

        public void setK(String k) {
            this.k = k;
        }

        public String getV() {
            return v;
        }

        public void setV(String v) {
            this.v = v;
        }

        @Override
        public String toString() {
            return "Argument{" +
                    "k='" + k + '\'' +
                    ", v=" + v +
                    '}';
        }
    }
}

package com.github.tonek.monitoringclient;

import java.util.Collections;
import java.util.Map;

class FullMetricKey {
    private final String groupName;
    private final String name;
    private final Map<String, Object> arguments;

    public FullMetricKey(String groupName, String name, Map<String, Object> arguments) {
        this.groupName = groupName;
        this.name = name;
        this.arguments = arguments == null
                ? Collections.<String, Object>emptyMap() : Collections.unmodifiableMap(arguments);
    }

    public String getGroupName() {
        return groupName;
    }

    public String getName() {
        return name;
    }

    public String toIdString() {
        return "gn:" + groupName + "|n:" + name + "|args:" + arguments;
    }

    public String toMetricId() {
        return (groupName + "_" + name).replaceAll(" ", "_");
    }

    public Map<String, Object> getArguments() {
        return arguments;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FullMetricKey that = (FullMetricKey) o;

        if (groupName != null ? !groupName.equals(that.groupName) : that.groupName != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        return !(arguments != null ? !arguments.equals(that.arguments) : that.arguments != null);

    }

    @Override
    public int hashCode() {
        int result = groupName != null ? groupName.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (arguments != null ? arguments.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "FullMetricKey{" +
                "groupName='" + groupName + '\'' +
                ", name='" + name + '\'' +
                ", arguments=" + arguments +
                '}';
    }
}

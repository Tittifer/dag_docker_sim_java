package com.dagdockersim.model.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class HealthVO {
    private String service;
    private String mode;
    private Map<String, Object> storage;
    private Map<String, Object> cloud;
    private List<FusionSummaryVO> fusions;

    @JsonProperty("simulated_device_count")
    private Integer simulatedDeviceCount;

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Map<String, Object> getStorage() {
        return storage;
    }

    public void setStorage(Map<String, Object> storage) {
        this.storage = storage;
    }

    public Map<String, Object> getCloud() {
        return cloud;
    }

    public void setCloud(Map<String, Object> cloud) {
        this.cloud = cloud;
    }

    public List<FusionSummaryVO> getFusions() {
        return fusions;
    }

    public void setFusions(List<FusionSummaryVO> fusions) {
        this.fusions = fusions;
    }

    public Integer getSimulatedDeviceCount() {
        return simulatedDeviceCount;
    }

    public void setSimulatedDeviceCount(Integer simulatedDeviceCount) {
        this.simulatedDeviceCount = simulatedDeviceCount;
    }
}

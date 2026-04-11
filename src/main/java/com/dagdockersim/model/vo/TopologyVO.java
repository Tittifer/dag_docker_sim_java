package com.dagdockersim.model.vo;

import java.util.List;
import java.util.Map;

public class TopologyVO {
    private Map<String, Object> storage;
    private Map<String, Object> cloud;
    private List<FusionSummaryVO> fusions;
    private List<DeviceVO> devices;

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

    public List<DeviceVO> getDevices() {
        return devices;
    }

    public void setDevices(List<DeviceVO> devices) {
        this.devices = devices;
    }
}

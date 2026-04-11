package com.dagdockersim.model.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class RegisterDeviceVO {
    private Boolean accepted;

    @JsonProperty("device_name")
    private String deviceName;

    @JsonProperty("device_id")
    private String deviceId;

    @JsonProperty("terminal_id")
    private String terminalId;

    @JsonProperty("register_tx")
    private Map<String, Object> registerTx;

    @JsonProperty("cloud_summary")
    private Map<String, Object> cloudSummary;

    @JsonProperty("fusion_summary")
    private Map<String, Object> fusionSummary;

    private Map<String, Object> storage;

    public Boolean getAccepted() {
        return accepted;
    }

    public void setAccepted(Boolean accepted) {
        this.accepted = accepted;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getTerminalId() {
        return terminalId;
    }

    public void setTerminalId(String terminalId) {
        this.terminalId = terminalId;
    }

    public Map<String, Object> getRegisterTx() {
        return registerTx;
    }

    public void setRegisterTx(Map<String, Object> registerTx) {
        this.registerTx = registerTx;
    }

    public Map<String, Object> getCloudSummary() {
        return cloudSummary;
    }

    public void setCloudSummary(Map<String, Object> cloudSummary) {
        this.cloudSummary = cloudSummary;
    }

    public Map<String, Object> getFusionSummary() {
        return fusionSummary;
    }

    public void setFusionSummary(Map<String, Object> fusionSummary) {
        this.fusionSummary = fusionSummary;
    }

    public Map<String, Object> getStorage() {
        return storage;
    }

    public void setStorage(Map<String, Object> storage) {
        this.storage = storage;
    }
}

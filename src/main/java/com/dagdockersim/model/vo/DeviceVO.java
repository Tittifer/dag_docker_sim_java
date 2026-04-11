package com.dagdockersim.model.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DeviceVO {
    @JsonProperty("device_id")
    private String deviceId;

    @JsonProperty("device_name")
    private String deviceName;

    @JsonProperty("terminal_id")
    private String terminalId;

    @JsonProperty("sign_pubkey")
    private String signPubkey;

    @JsonProperty("bootstrap_identity")
    private Boolean bootstrapIdentity;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getTerminalId() {
        return terminalId;
    }

    public void setTerminalId(String terminalId) {
        this.terminalId = terminalId;
    }

    public String getSignPubkey() {
        return signPubkey;
    }

    public void setSignPubkey(String signPubkey) {
        this.signPubkey = signPubkey;
    }

    public Boolean getBootstrapIdentity() {
        return bootstrapIdentity;
    }

    public void setBootstrapIdentity(Boolean bootstrapIdentity) {
        this.bootstrapIdentity = bootstrapIdentity;
    }
}

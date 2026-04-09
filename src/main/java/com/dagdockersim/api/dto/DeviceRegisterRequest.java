package com.dagdockersim.api.dto;

import javax.validation.constraints.NotBlank;

public class DeviceRegisterRequest {
    @NotBlank
    private String deviceName;

    private Boolean useBootstrapIdentity = Boolean.FALSE;
    private Boolean autoConfirm = Boolean.TRUE;

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public Boolean getUseBootstrapIdentity() {
        return useBootstrapIdentity;
    }

    public void setUseBootstrapIdentity(Boolean useBootstrapIdentity) {
        this.useBootstrapIdentity = useBootstrapIdentity;
    }

    public Boolean getAutoConfirm() {
        return autoConfirm;
    }

    public void setAutoConfirm(Boolean autoConfirm) {
        this.autoConfirm = autoConfirm;
    }
}

package com.dagdockersim.simulation.infrastructure.persistence.session.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(
    name = "device_sessions",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_device_session_device_id", columnNames = {"device_id"})
    }
)
public class DeviceSessionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = 128)
    private String deviceId;

    @Column(name = "device_name", nullable = false, length = 128)
    private String deviceName;

    @Column(name = "terminal_id", nullable = false, length = 64)
    private String terminalId;

    @Lob
    @Column(name = "sign_privkey", nullable = false, columnDefinition = "LONGTEXT")
    private String signPrivkey;

    @Lob
    @Column(name = "sign_pubkey", nullable = false, columnDefinition = "LONGTEXT")
    private String signPubkey;

    @Column(name = "bootstrap_identity", nullable = false)
    private Boolean bootstrapIdentity;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getSignPrivkey() {
        return signPrivkey;
    }

    public void setSignPrivkey(String signPrivkey) {
        this.signPrivkey = signPrivkey;
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

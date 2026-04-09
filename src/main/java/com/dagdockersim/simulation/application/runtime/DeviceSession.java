package com.dagdockersim.simulation.application.runtime;

import com.dagdockersim.device.DeviceSimulator;
import com.dagdockersim.simulation.infrastructure.persistence.session.DeviceSessionStore;

public class DeviceSession {
    private final DeviceSimulator simulator;
    private final String terminalId;
    private final boolean bootstrapIdentity;

    public DeviceSession(DeviceSimulator simulator, String terminalId, boolean bootstrapIdentity) {
        this.simulator = simulator;
        this.terminalId = terminalId;
        this.bootstrapIdentity = bootstrapIdentity;
    }

    public DeviceSimulator getSimulator() {
        return simulator;
    }

    public String getTerminalId() {
        return terminalId;
    }

    public boolean isBootstrapIdentity() {
        return bootstrapIdentity;
    }

    public DeviceSessionStore.DeviceSessionSnapshot toSnapshot() {
        return new DeviceSessionStore.DeviceSessionSnapshot(
            simulator.getDeviceId(),
            simulator.getDeviceName(),
            terminalId,
            simulator.getSignPrivkey(),
            simulator.getSignPubkey(),
            bootstrapIdentity
        );
    }
}

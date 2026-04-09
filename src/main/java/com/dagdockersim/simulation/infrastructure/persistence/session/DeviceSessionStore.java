package com.dagdockersim.simulation.infrastructure.persistence.session;

import com.dagdockersim.simulation.infrastructure.persistence.session.entity.DeviceSessionEntity;
import com.dagdockersim.simulation.infrastructure.persistence.session.repository.DeviceSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class DeviceSessionStore {
    private final DeviceSessionRepository deviceSessionRepository;

    public DeviceSessionStore(DeviceSessionRepository deviceSessionRepository) {
        this.deviceSessionRepository = deviceSessionRepository;
    }

    public List<DeviceSessionSnapshot> loadAll() {
        List<DeviceSessionSnapshot> snapshots = new ArrayList<DeviceSessionSnapshot>();
        for (DeviceSessionEntity entity : deviceSessionRepository.findAllByOrderByTerminalIdAscDeviceNameAsc()) {
            snapshots.add(
                new DeviceSessionSnapshot(
                    entity.getDeviceId(),
                    entity.getDeviceName(),
                    entity.getTerminalId(),
                    entity.getSignPrivkey(),
                    entity.getSignPubkey(),
                    Boolean.TRUE.equals(entity.getBootstrapIdentity())
                )
            );
        }
        return snapshots;
    }

    @Transactional
    public void save(DeviceSessionSnapshot snapshot) {
        DeviceSessionEntity entity = new DeviceSessionEntity();
        entity.setDeviceId(snapshot.getDeviceId());
        entity.setDeviceName(snapshot.getDeviceName());
        entity.setTerminalId(snapshot.getTerminalId());
        entity.setSignPrivkey(snapshot.getSignPrivkey());
        entity.setSignPubkey(snapshot.getSignPubkey());
        entity.setBootstrapIdentity(Boolean.valueOf(snapshot.isBootstrapIdentity()));
        deviceSessionRepository.save(entity);
    }

    public static class DeviceSessionSnapshot {
        private final String deviceId;
        private final String deviceName;
        private final String terminalId;
        private final String signPrivkey;
        private final String signPubkey;
        private final boolean bootstrapIdentity;

        public DeviceSessionSnapshot(
            String deviceId,
            String deviceName,
            String terminalId,
            String signPrivkey,
            String signPubkey,
            boolean bootstrapIdentity
        ) {
            this.deviceId = deviceId;
            this.deviceName = deviceName;
            this.terminalId = terminalId;
            this.signPrivkey = signPrivkey;
            this.signPubkey = signPubkey;
            this.bootstrapIdentity = bootstrapIdentity;
        }

        public String getDeviceId() {
            return deviceId;
        }

        public String getDeviceName() {
            return deviceName;
        }

        public String getTerminalId() {
            return terminalId;
        }

        public String getSignPrivkey() {
            return signPrivkey;
        }

        public String getSignPubkey() {
            return signPubkey;
        }

        public boolean isBootstrapIdentity() {
            return bootstrapIdentity;
        }
    }
}

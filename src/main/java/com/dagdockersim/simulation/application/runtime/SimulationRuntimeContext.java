package com.dagdockersim.simulation.application.runtime;

import com.dagdockersim.cloud.CloudStation;
import com.dagdockersim.device.DeviceSimulator;
import com.dagdockersim.fusion.FusionTerminal;
import com.dagdockersim.shared.model.LifecycleAction;
import com.dagdockersim.shared.model.Transaction;
import com.dagdockersim.simulation.api.dto.DeviceRegisterRequest;
import com.dagdockersim.simulation.api.dto.TelemetrySubmitRequest;
import com.dagdockersim.simulation.infrastructure.persistence.ledger.LedgerStateStore;
import com.dagdockersim.simulation.infrastructure.persistence.session.DeviceSessionStore;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SimulationRuntimeContext {
    private static final String CLOUD_TERMINAL_ID = "cloud";

    private final boolean cloudSnapshotRestored;
    private final LedgerStateStore ledgerStateStore;
    private final DeviceSessionStore deviceSessionStore;
    private final CloudStation cloud;
    private final Map<String, FusionTerminal> fusions;
    private final Map<String, DeviceSession> deviceSessions = new LinkedHashMap<String, DeviceSession>();

    public SimulationRuntimeContext(LedgerStateStore ledgerStateStore, DeviceSessionStore deviceSessionStore) {
        this.ledgerStateStore = ledgerStateStore;
        this.deviceSessionStore = deviceSessionStore;

        this.cloudSnapshotRestored = ledgerStateStore.hasTerminalSnapshot(CLOUD_TERMINAL_ID);
        CloudStation initializedCloud = new CloudStation(30, 5, true, !cloudSnapshotRestored);
        FusionTerminal fusion1 = new FusionTerminal("fusion1", initializedCloud, !cloudSnapshotRestored);
        FusionTerminal fusion2 = new FusionTerminal("fusion2", initializedCloud, !cloudSnapshotRestored);
        FusionTerminal fusion3 = new FusionTerminal("fusion3", initializedCloud, !cloudSnapshotRestored);

        initializedCloud.attachFusion(fusion1);
        initializedCloud.attachFusion(fusion2);
        initializedCloud.attachFusion(fusion3);

        this.cloud = initializedCloud;
        this.fusions = new LinkedHashMap<String, FusionTerminal>();
        this.fusions.put(fusion1.getTerminalId(), fusion1);
        this.fusions.put(fusion2.getTerminalId(), fusion2);
        this.fusions.put(fusion3.getTerminalId(), fusion3);

        if (cloudSnapshotRestored) {
            restoreLedger(CLOUD_TERMINAL_ID, cloud);
            for (FusionTerminal fusion : fusions.values()) {
                restoreLedger(fusion.getTerminalId(), fusion);
            }
            restoreDeviceSessions();
        } else {
            syncAllLedgers();
        }
    }

    public synchronized Map<String, Object> health() {
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("service", "dag-docker-sim-java");
        response.put("mode", "spring-boot-rest");
        response.put("storage", storageSummary());
        response.put("cloud", cloud.getLedger().summary());
        response.put("fusions", fusionSummaries());
        response.put("simulated_device_count", Integer.valueOf(deviceSessions.size()));
        return response;
    }

    public synchronized Map<String, Object> topology() {
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("storage", storageSummary());
        response.put("cloud", cloud.getLedger().summary());
        response.put("fusions", fusionSummaries());
        response.put("devices", listDevices());
        return response;
    }

    public synchronized Map<String, Object> cloudLedger() {
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("summary", cloud.getLedger().summary());
        response.put("transactions", transactionsOf(cloud.getLedger().getTxIndex().values()));
        response.put("archive_size", Integer.valueOf(cloud.getArchive().size()));
        response.put("storage", storageSummary());
        return response;
    }

    public synchronized List<Map<String, Object>> listFusions() {
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (FusionTerminal fusion : fusions.values()) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("terminal_id", fusion.getTerminalId());
            item.put("summary", fusion.getLedger().summary());
            items.add(item);
        }
        return items;
    }

    public synchronized Map<String, Object> fusionLedger(String terminalId) {
        FusionTerminal fusion = requireFusion(terminalId);
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("terminal_id", terminalId);
        response.put("summary", fusion.getLedger().summary());
        response.put("transactions", transactionsOf(fusion.getLedger().getTxIndex().values()));
        response.put("storage", storageSummary());
        return response;
    }

    public synchronized Map<String, Object> registerDevice(String terminalId, DeviceRegisterRequest request) {
        FusionTerminal fusion = requireFusion(terminalId);
        DeviceSimulator simulator = Boolean.TRUE.equals(request.getUseBootstrapIdentity())
            ? DeviceSimulator.newBootstrap(request.getDeviceName())
            : DeviceSimulator.newDynamic(request.getDeviceName());

        Transaction registerTx;
        try {
            registerTx = simulator.registerAt(fusion);
        } catch (IllegalStateException exception) {
            throw mapDomainError(exception);
        }

        DeviceSession session = new DeviceSession(
            simulator,
            terminalId,
            Boolean.TRUE.equals(request.getUseBootstrapIdentity())
        );
        deviceSessions.put(simulator.getDeviceId(), session);
        deviceSessionStore.save(session.toSnapshot());

        if (Boolean.TRUE.equals(request.getAutoConfirm())) {
            confirmRegister(registerTx.getTxId());
        }

        syncAllLedgers();

        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("accepted", Boolean.TRUE);
        response.put("device_name", simulator.getDeviceName());
        response.put("device_id", simulator.getDeviceId());
        response.put("terminal_id", terminalId);
        response.put("register_tx", registerTx.toMap());
        response.put("cloud_summary", cloud.getLedger().summary());
        response.put("fusion_summary", fusion.getLedger().summary());
        response.put("storage", storageSummary());
        return response;
    }

    public synchronized Map<String, Object> submitTelemetry(String terminalId, String deviceId, TelemetrySubmitRequest request) {
        FusionTerminal fusion = requireFusion(terminalId);
        DeviceSession session = requireDevice(deviceId);
        if (!terminalId.equals(session.getTerminalId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "device_terminal_mismatch");
        }

        Map<String, Object> payload = request.getDataPayload() == null
            ? defaultTelemetry(session.getSimulator())
            : new LinkedHashMap<String, Object>(request.getDataPayload());

        if (!payload.containsKey("device_name")) {
            payload.put("device_name", session.getSimulator().getDeviceName());
        }
        if (!payload.containsKey("captured_at")) {
            payload.put("captured_at", Double.valueOf(System.currentTimeMillis() / 1000.0));
        }
        if (!payload.containsKey("metrics")) {
            payload.put("metrics", defaultMetrics());
        }

        Transaction businessTx;
        try {
            businessTx = session.getSimulator().submitTelemetry(fusion, payload);
        } catch (IllegalStateException exception) {
            throw mapDomainError(exception);
        }

        syncAllLedgers();

        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("accepted", Boolean.TRUE);
        response.put("device_id", deviceId);
        response.put("terminal_id", terminalId);
        response.put("business_tx", businessTx.toMap());
        response.put("cloud_summary", cloud.getLedger().summary());
        response.put("fusion_summary", fusion.getLedger().summary());
        response.put("storage", storageSummary());
        return response;
    }

    public synchronized List<Map<String, Object>> listDevices() {
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (Map.Entry<String, DeviceSession> entry : deviceSessions.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("device_id", entry.getKey());
            item.put("device_name", entry.getValue().getSimulator().getDeviceName());
            item.put("terminal_id", entry.getValue().getTerminalId());
            item.put("sign_pubkey", entry.getValue().getSimulator().getSignPubkey());
            item.put("bootstrap_identity", Boolean.valueOf(entry.getValue().isBootstrapIdentity()));
            items.add(item);
        }
        return items;
    }

    private void confirmRegister(String txId) {
        cloud.getLedger().confirmRegister(txId);
        LifecycleAction confirmAction = new LifecycleAction("confirm_register", txId);
        for (FusionTerminal fusion : fusions.values()) {
            fusion.applyConfirmation(confirmAction);
        }
    }

    private void restoreLedger(String terminalId, CloudStation cloudStation) {
        List<Transaction> transactions = ledgerStateStore.loadLedger(terminalId);
        for (Transaction transaction : transactions) {
            if ("genesis".equals(transaction.getTxType())) {
                cloudStation.archiveTransaction(transaction);
                continue;
            }
            cloudStation.getLedger().insertTransaction(transaction.copy());
            cloudStation.archiveTransaction(transaction);
        }
    }

    private void restoreLedger(String terminalId, FusionTerminal fusionTerminal) {
        List<Transaction> transactions = ledgerStateStore.loadLedger(terminalId);
        for (Transaction transaction : transactions) {
            if ("genesis".equals(transaction.getTxType())) {
                continue;
            }
            fusionTerminal.getLedger().insertTransaction(transaction.copy());
        }
    }

    private void syncAllLedgers() {
        ledgerStateStore.replaceLedger(CLOUD_TERMINAL_ID, cloud.getLedger().getTxIndex().values());
        for (FusionTerminal fusion : fusions.values()) {
            ledgerStateStore.replaceLedger(fusion.getTerminalId(), fusion.getLedger().getTxIndex().values());
        }
    }

    private void restoreDeviceSessions() {
        for (DeviceSessionStore.DeviceSessionSnapshot snapshot : deviceSessionStore.loadAll()) {
            if (!fusions.containsKey(snapshot.getTerminalId())) {
                continue;
            }
            DeviceSimulator simulator = DeviceSimulator.restore(
                snapshot.getDeviceName(),
                snapshot.getDeviceId(),
                snapshot.getSignPrivkey(),
                snapshot.getSignPubkey()
            );
            deviceSessions.put(
                snapshot.getDeviceId(),
                new DeviceSession(simulator, snapshot.getTerminalId(), snapshot.isBootstrapIdentity())
            );
        }
    }

    private Map<String, Object> storageSummary() {
        Map<String, Object> storage = new LinkedHashMap<String, Object>();
        storage.put("ledger_persistence", "mysql");
        storage.put("read_cache", "redis");
        storage.put("cloud_snapshot_restored", Boolean.valueOf(cloudSnapshotRestored));
        storage.put("restored_device_session_count", Integer.valueOf(deviceSessions.size()));
        return storage;
    }

    private FusionTerminal requireFusion(String terminalId) {
        FusionTerminal fusion = fusions.get(terminalId);
        if (fusion == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "terminal_not_found");
        }
        return fusion;
    }

    private DeviceSession requireDevice(String deviceId) {
        DeviceSession session = deviceSessions.get(deviceId);
        if (session == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "device_not_found");
        }
        return session;
    }

    private ResponseStatusException mapDomainError(IllegalStateException exception) {
        String message = exception.getMessage() == null ? "simulation_error" : exception.getMessage();
        if ("duplicate_device_id".equals(message)) {
            return new ResponseStatusException(HttpStatus.CONFLICT, message);
        }
        if ("device_not_registered".equals(message)) {
            return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
        }
        if ("device_identity_not_confirmed".equals(message)) {
            return new ResponseStatusException(HttpStatus.CONFLICT, message);
        }
        if ("device_signature_invalid".equals(message)) {
            return new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
        }
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private List<Map<String, Object>> fusionSummaries() {
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (FusionTerminal fusion : fusions.values()) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("terminal_id", fusion.getTerminalId());
            item.put("summary", fusion.getLedger().summary());
            items.add(item);
        }
        return items;
    }

    private List<Map<String, Object>> transactionsOf(Collection<Transaction> transactions) {
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (Transaction transaction : transactions) {
            items.add(transaction.toMap());
        }
        return items;
    }

    private Map<String, Object> defaultTelemetry(DeviceSimulator simulator) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("sequence", Integer.valueOf(1));
        payload.put("device_name", simulator.getDeviceName());
        payload.put("captured_at", Double.valueOf(System.currentTimeMillis() / 1000.0));
        payload.put("metrics", defaultMetrics());
        return payload;
    }

    private Map<String, Object> defaultMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<String, Object>();
        metrics.put("voltage_v", Double.valueOf(228.4));
        metrics.put("current_a", Double.valueOf(16.2));
        metrics.put("temperature_c", Double.valueOf(33.8));
        metrics.put("active_power_kw", Double.valueOf(3.701));
        return metrics;
    }

}

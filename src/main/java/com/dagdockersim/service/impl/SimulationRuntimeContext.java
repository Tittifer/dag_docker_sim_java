package com.dagdockersim.service.impl;

import com.dagdockersim.core.cloud.CloudStation;
import com.dagdockersim.core.device.DeviceSimulator;
import com.dagdockersim.core.fusion.FusionTerminal;
import com.dagdockersim.model.domain.LifecycleAction;
import com.dagdockersim.model.domain.Transaction;
import com.dagdockersim.model.request.DeviceRegisterRequest;
import com.dagdockersim.model.request.TelemetrySubmitRequest;
import com.dagdockersim.service.impl.support.AsyncLedgerPersistenceCoordinator;
import com.dagdockersim.service.impl.support.DeviceSessionStore;
import com.dagdockersim.service.impl.support.LedgerStateStore;
import com.dagdockersim.service.impl.support.TerminalTaskDispatcher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SimulationRuntimeContext {
    private static final String CLOUD_TERMINAL_ID = "cloud";

    private final boolean cloudSnapshotRestored;
    private final LedgerStateStore ledgerStateStore;
    private final DeviceSessionStore deviceSessionStore;
    private final AsyncLedgerPersistenceCoordinator ledgerPersistenceCoordinator;
    private final CloudStation cloud;
    private final Map<String, FusionTerminal> fusions;
    private final Map<String, DeviceSession> deviceSessions = new ConcurrentHashMap<String, DeviceSession>();
    private final TerminalTaskDispatcher taskDispatcher;

    public SimulationRuntimeContext(
        LedgerStateStore ledgerStateStore,
        DeviceSessionStore deviceSessionStore,
        AsyncLedgerPersistenceCoordinator ledgerPersistenceCoordinator
    ) {
        this.ledgerStateStore = ledgerStateStore;
        this.deviceSessionStore = deviceSessionStore;
        this.ledgerPersistenceCoordinator = ledgerPersistenceCoordinator;

        this.cloudSnapshotRestored = ledgerStateStore.hasTerminalSnapshot(CLOUD_TERMINAL_ID);
        this.cloud = new CloudStation(30, 5, true, !cloudSnapshotRestored);
        FusionTerminal fusion1 = new FusionTerminal("fusion1", !cloudSnapshotRestored);
        FusionTerminal fusion2 = new FusionTerminal("fusion2", !cloudSnapshotRestored);
        FusionTerminal fusion3 = new FusionTerminal("fusion3", !cloudSnapshotRestored);

        this.fusions = new LinkedHashMap<String, FusionTerminal>();
        this.fusions.put(fusion1.getTerminalId(), fusion1);
        this.fusions.put(fusion2.getTerminalId(), fusion2);
        this.fusions.put(fusion3.getTerminalId(), fusion3);
        this.taskDispatcher = new TerminalTaskDispatcher(terminalIds());

        if (cloudSnapshotRestored) {
            restoreLedger(CLOUD_TERMINAL_ID, cloud);
            for (FusionTerminal fusion : fusions.values()) {
                restoreLedger(fusion.getTerminalId(), fusion);
            }
            restoreDeviceSessions();
        } else {
            syncInitialLedgers();
        }
    }

    public Map<String, Object> health() {
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("service", "dag-docker-sim-java");
        response.put("mode", "spring-boot-rest");
        response.put("storage", storageSummary());
        response.put("cloud", cloudSummary());
        response.put("fusions", fusionSummaries());
        response.put("simulated_device_count", Integer.valueOf(deviceSessions.size()));
        return response;
    }

    public Map<String, Object> topology() {
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("storage", storageSummary());
        response.put("cloud", cloudSummary());
        response.put("fusions", fusionSummaries());
        response.put("devices", listDevices());
        return response;
    }

    public Map<String, Object> cloudLedger() {
        return taskDispatcher.call(CLOUD_TERMINAL_ID, () -> {
            Map<String, Object> response = new LinkedHashMap<String, Object>();
            response.put("summary", cloud.getLedger().summary());
            response.put("transactions", transactionsOf(cloud.getLedger().getTxIndex().values()));
            response.put("archive_size", Integer.valueOf(cloud.getArchive().size()));
            response.put("storage", storageSummary());
            return response;
        });
    }

    public List<Map<String, Object>> listFusions() {
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (FusionTerminal fusion : fusions.values()) {
            items.add(taskDispatcher.call(fusion.getTerminalId(), () -> fusionSummary(fusion)));
        }
        return items;
    }

    public Map<String, Object> fusionLedger(String terminalId) {
        FusionTerminal fusion = requireFusion(terminalId);
        return taskDispatcher.call(terminalId, () -> {
            Map<String, Object> response = new LinkedHashMap<String, Object>();
            response.put("terminal_id", terminalId);
            response.put("summary", fusion.getLedger().summary());
            response.put("transactions", transactionsOf(fusion.getLedger().getTxIndex().values()));
            response.put("storage", storageSummary());
            return response;
        });
    }

    public Map<String, Object> registerDevice(String terminalId, DeviceRegisterRequest request) {
        FusionTerminal fusion = requireFusion(terminalId);
        DeviceSimulator simulator = Boolean.TRUE.equals(request.getUseBootstrapIdentity())
            ? DeviceSimulator.newBootstrap(request.getDeviceName())
            : DeviceSimulator.newDynamic(request.getDeviceName());

        Transaction registerTx = taskDispatcher.call(terminalId, () -> fusion.registerDevice(
            simulator.getDeviceId(),
            simulator.getSignPubkey()
        ));

        DeviceSession session = new DeviceSession(
            simulator,
            terminalId,
            Boolean.TRUE.equals(request.getUseBootstrapIdentity())
        );
        deviceSessions.put(simulator.getDeviceId(), session);
        deviceSessionStore.save(session.toSnapshot());

        broadcastToCloudAndPeers(registerTx);

        if (Boolean.TRUE.equals(request.getAutoConfirm())) {
            confirmRegister(registerTx.getTxId());
        }

        persistAllLedgers();

        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("accepted", Boolean.TRUE);
        response.put("device_name", simulator.getDeviceName());
        response.put("device_id", simulator.getDeviceId());
        response.put("terminal_id", terminalId);
        response.put("register_tx", registerTx.toMap());
        response.put("cloud_summary", cloudSummary());
        response.put("fusion_summary", taskDispatcher.call(terminalId, () -> fusion.getLedger().summary()));
        response.put("storage", storageSummary());
        return response;
    }

    public Map<String, Object> submitTelemetry(String terminalId, String deviceId, TelemetrySubmitRequest request) {
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

        Transaction businessTx = taskDispatcher.call(terminalId, () -> session.getSimulator().submitTelemetry(fusion, payload));

        broadcastToCloudAndPeers(businessTx);
        persistAllLedgers();

        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("accepted", Boolean.TRUE);
        response.put("device_id", deviceId);
        response.put("terminal_id", terminalId);
        response.put("business_tx", businessTx.toMap());
        response.put("cloud_summary", cloudSummary());
        response.put("fusion_summary", taskDispatcher.call(terminalId, () -> fusion.getLedger().summary()));
        response.put("storage", storageSummary());
        return response;
    }

    public List<Map<String, Object>> listDevices() {
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        List<String> deviceIds = new ArrayList<String>(deviceSessions.keySet());
        Collections.sort(deviceIds);
        for (String deviceId : deviceIds) {
            DeviceSession session = deviceSessions.get(deviceId);
            if (session == null) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("device_id", deviceId);
            item.put("device_name", session.getSimulator().getDeviceName());
            item.put("terminal_id", session.getTerminalId());
            item.put("sign_pubkey", session.getSimulator().getSignPubkey());
            item.put("bootstrap_identity", Boolean.valueOf(session.isBootstrapIdentity()));
            items.add(item);
        }
        return items;
    }

    @PreDestroy
    public void destroy() {
        if (taskDispatcher != null) {
            taskDispatcher.shutdown();
        }
    }

    private void broadcastToCloudAndPeers(Transaction transaction) {
        CloudStation.CloudBroadcastOutcome outcome = taskDispatcher.call(
            CLOUD_TERMINAL_ID,
            () -> cloud.receiveBroadcast(transaction.copy())
        );

        for (FusionTerminal fusion : fusions.values()) {
            if (fusion.getTerminalId().equals(transaction.getSourceTerminalId())) {
                continue;
            }
            taskDispatcher.run(fusion.getTerminalId(), () -> fusion.receiveBroadcast(transaction.copy()));
        }

        if (!outcome.getActions().isEmpty()) {
            applyActionsToFusions(outcome.getActions());
        }
    }

    private void confirmRegister(String txId) {
        taskDispatcher.call(CLOUD_TERMINAL_ID, () -> {
            cloud.getLedger().confirmRegister(txId);
            return Boolean.TRUE;
        });
        applyActionsToFusions(Collections.singletonList(new LifecycleAction("confirm_register", txId)));
    }

    private void applyActionsToFusions(List<LifecycleAction> actions) {
        for (LifecycleAction action : actions) {
            for (FusionTerminal fusion : fusions.values()) {
                taskDispatcher.run(fusion.getTerminalId(), () -> fusion.applyConfirmation(action));
            }
        }
    }

    private void persistAllLedgers() {
        ledgerPersistenceCoordinator.persistLedgers(collectLedgerSnapshots());
    }

    private void syncInitialLedgers() {
        ledgerPersistenceCoordinator.persistLedgersAndWait(collectLedgerSnapshots());
    }

    private List<Transaction> copyTransactions(Collection<Transaction> transactions) {
        List<Transaction> copies = new ArrayList<Transaction>();
        for (Transaction transaction : transactions) {
            copies.add(transaction.copy());
        }
        return copies;
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
        storage.put("ledger_write_mode", "async_mysql_snapshot");
        storage.put("read_cache", "redis");
        storage.put("cloud_snapshot_restored", Boolean.valueOf(cloudSnapshotRestored));
        storage.put("restored_device_session_count", Integer.valueOf(deviceSessions.size()));
        storage.put("async_persistence", ledgerPersistenceCoordinator.summary());
        return storage;
    }

    private Map<String, Object> cloudSummary() {
        return taskDispatcher.call(CLOUD_TERMINAL_ID, () -> cloud.getLedger().summary());
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

    private List<Map<String, Object>> fusionSummaries() {
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (FusionTerminal fusion : fusions.values()) {
            items.add(taskDispatcher.call(fusion.getTerminalId(), () -> fusionSummary(fusion)));
        }
        return items;
    }

    private Map<String, Object> fusionSummary(FusionTerminal fusion) {
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("terminal_id", fusion.getTerminalId());
        item.put("summary", fusion.getLedger().summary());
        return item;
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

    private List<String> terminalIds() {
        List<String> terminalIds = new ArrayList<String>();
        terminalIds.add(CLOUD_TERMINAL_ID);
        terminalIds.addAll(fusions.keySet());
        return terminalIds;
    }

    private Map<String, List<Transaction>> collectLedgerSnapshots() {
        Map<String, List<Transaction>> snapshots = new LinkedHashMap<String, List<Transaction>>();
        snapshots.put(
            CLOUD_TERMINAL_ID,
            taskDispatcher.call(CLOUD_TERMINAL_ID, () -> copyTransactions(cloud.getLedger().getTxIndex().values()))
        );
        for (FusionTerminal fusion : fusions.values()) {
            final FusionTerminal currentFusion = fusion;
            snapshots.put(
                currentFusion.getTerminalId(),
                taskDispatcher.call(
                    currentFusion.getTerminalId(),
                    () -> copyTransactions(currentFusion.getLedger().getTxIndex().values())
                )
            );
        }
        return snapshots;
    }
}

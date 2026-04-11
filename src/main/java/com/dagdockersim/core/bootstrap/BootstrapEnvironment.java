package com.dagdockersim.core.bootstrap;

import com.dagdockersim.core.crypto.CryptoUtils;
import com.dagdockersim.core.crypto.KeyPairStrings;
import com.dagdockersim.core.ledger.DagLedger;
import com.dagdockersim.model.domain.Transaction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BootstrapEnvironment {
    private static final KeyPairStrings BOOTSTRAP_SIGNER = CryptoUtils.deriveSecp256k1KeyPair("bootstrap::signer");
    private static final double BOOTSTRAP_BASE_TS = 1773820000.0;
    private static final Map<String, List<String>> PRELOADED_DEVICES_BY_TERMINAL = createPreloadedDevices();

    private BootstrapEnvironment() {
    }

    public static Map<String, List<String>> preloadedDevicesByTerminal() {
        Map<String, List<String>> result = new LinkedHashMap<String, List<String>>();
        for (Map.Entry<String, List<String>> entry : PRELOADED_DEVICES_BY_TERMINAL.entrySet()) {
            result.put(entry.getKey(), new ArrayList<String>(entry.getValue()));
        }
        return result;
    }

    public static String bootstrapDeviceId(String deviceName) {
        return CryptoUtils.sha256Hex("bootstrap-device-id::" + deviceName).substring(0, 20);
    }

    public static KeyPairStrings bootstrapDeviceKeyPair(String deviceName) {
        return CryptoUtils.deriveSecp256k1KeyPair("bootstrap-device-key::" + deviceName);
    }

    public static String bootstrapDevicePubkey(String deviceName) {
        return bootstrapDeviceKeyPair(deviceName).getPublicKey();
    }

    public static List<Transaction> generateBootstrapTransactions() {
        List<Transaction> transactions = new ArrayList<Transaction>();
        int currentRound = 0;
        List<String> registerTips = new ArrayList<String>(DagLedger.GLOBAL_GENESIS_IDS);

        for (Map.Entry<String, List<String>> entry : preloadedDevicesByTerminal().entrySet()) {
            String terminalId = entry.getKey();
            for (String deviceName : entry.getValue()) {
                currentRound += 1;
                String deviceId = bootstrapDeviceId(deviceName);
                String devicePubkey = bootstrapDevicePubkey(deviceName);

                Map<String, Object> attributes = new LinkedHashMap<String, Object>();
                attributes.put("role", "edge_device");
                attributes.put("access_domain", terminalId);
                attributes.put("trust_level", "normal");
                attributes.put("device_group", deviceName.substring(Math.max(0, deviceName.length() - 2)));
                attributes.put("bootstrap_preloaded", Boolean.TRUE);
                attributes.put("globally_confirmed", Boolean.TRUE);

                List<String> parentIds = new ArrayList<String>();
                int from = Math.max(0, registerTips.size() - 2);
                parentIds.addAll(registerTips.subList(from, registerTips.size()));

                Transaction tx = buildSignedTx(
                    "register",
                    terminalId,
                    parentIds,
                    currentRound,
                    BOOTSTRAP_BASE_TS + currentRound,
                    payloadBody(deviceId, devicePubkey, terminalId, attributes),
                    deviceId,
                    devicePubkey,
                    terminalId,
                    attributes
                );
                tx.setStatus("confirmed");
                tx.setCumulativeWeight(30);
                tx.setConfirmedAt(Double.valueOf(tx.getTimestamp()));
                registerTips.add(tx.getTxId());
                transactions.add(tx);
            }
        }

        transactions.sort((left, right) -> {
            int byTime = Double.compare(left.getTimestamp(), right.getTimestamp());
            if (byTime != 0) {
                return byTime;
            }
            return left.getTxId().compareTo(right.getTxId());
        });
        return transactions;
    }

    public static List<Transaction> globalBootstrapTransactions() {
        return copyTransactions(generateBootstrapTransactions());
    }

    public static List<Transaction> localBootstrapTransactions(String terminalId) {
        return globalBootstrapTransactions();
    }

    public static List<Transaction> seedBootstrapLedger(DagLedger ledger, String scope) {
        List<Transaction> source = "cloud".equals(scope)
            ? globalBootstrapTransactions()
            : localBootstrapTransactions(ledger.getTerminalId());
        List<Transaction> inserted = new ArrayList<Transaction>();
        for (Transaction tx : source) {
            if (ledger.insertTransaction(tx)) {
                inserted.add(tx);
            }
        }
        return inserted;
    }

    private static Transaction buildSignedTx(
        String txType,
        String sourceTerminalId,
        List<String> parentIds,
        int createdRound,
        double timestamp,
        Map<String, Object> payloadBody,
        String deviceId,
        String devicePubkey,
        String targetTerminalId,
        Map<String, Object> attributes
    ) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("tx_type", txType);
        payload.put("parent_ids", new ArrayList<String>(parentIds));
        payload.put("source_terminal_id", sourceTerminalId);
        payload.put("source_terminal_pubkey", BOOTSTRAP_SIGNER.getPublicKey());
        payload.put("round", Integer.valueOf(createdRound));
        payload.put("timestamp", Double.valueOf(timestamp));
        payload.putAll(payloadBody);

        Transaction tx = new Transaction();
        tx.setTxId(CryptoUtils.sha256Hex(payload));
        tx.setTxType(txType);
        tx.setSourceTerminalId(sourceTerminalId);
        tx.setSourceTerminalPubkey(BOOTSTRAP_SIGNER.getPublicKey());
        tx.setParentIds(new ArrayList<String>(parentIds));
        tx.setTimestamp(timestamp);
        tx.setCreatedRound(createdRound);
        tx.setPayloadHash(CryptoUtils.sha256Hex(payload));
        tx.setTerminalSignature(CryptoUtils.secpSign(BOOTSTRAP_SIGNER.getPrivateKey(), payload));
        tx.setDeviceId(deviceId);
        tx.setDevicePubkey(devicePubkey);
        tx.setTargetTerminalId(targetTerminalId);
        tx.setAttributes(attributes);
        tx.setPayload(payload);
        return tx;
    }

    private static Map<String, Object> payloadBody(
        String deviceId,
        String devicePubkey,
        String terminalId,
        Map<String, Object> attributes
    ) {
        Map<String, Object> payloadBody = new LinkedHashMap<String, Object>();
        payloadBody.put("device_id", deviceId);
        payloadBody.put("device_pubkey", devicePubkey);
        payloadBody.put("target_terminal_id", terminalId);
        payloadBody.put("attributes", attributes);
        return payloadBody;
    }

    private static List<Transaction> copyTransactions(List<Transaction> source) {
        List<Transaction> copies = new ArrayList<Transaction>();
        for (Transaction tx : source) {
            copies.add(tx.copy());
        }
        return copies;
    }

    private static Map<String, List<String>> createPreloadedDevices() {
        Map<String, List<String>> result = new LinkedHashMap<String, List<String>>();
        result.put("fusion1", asList("device11", "device12", "device13"));
        result.put("fusion2", asList("device21", "device22", "device23"));
        result.put("fusion3", asList("device31", "device32", "device33"));
        return result;
    }

    private static List<String> asList(String... values) {
        List<String> list = new ArrayList<String>();
        for (String value : values) {
            list.add(value);
        }
        return list;
    }
}


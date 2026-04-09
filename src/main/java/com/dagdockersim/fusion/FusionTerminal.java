package com.dagdockersim.fusion;

import com.dagdockersim.cloud.CloudStation;
import com.dagdockersim.shared.bootstrap.BootstrapEnvironment;
import com.dagdockersim.shared.crypto.CryptoUtils;
import com.dagdockersim.shared.crypto.KeyPairStrings;
import com.dagdockersim.shared.ledger.DagLedger;
import com.dagdockersim.shared.model.LifecycleAction;
import com.dagdockersim.shared.model.Transaction;

import java.util.LinkedHashMap;
import java.util.Map;

public class FusionTerminal {
    private final String terminalId;
    private final CloudStation cloudStation;
    private final String signPrivkey;
    private final String signPubkey;
    private final DagLedger ledger;

    public FusionTerminal(String terminalId, CloudStation cloudStation) {
        this.terminalId = terminalId;
        this.cloudStation = cloudStation;
        KeyPairStrings signer = CryptoUtils.generateSecp256k1KeyPair();
        this.signPrivkey = signer.getPrivateKey();
        this.signPubkey = signer.getPublicKey();
        this.ledger = new DagLedger(
            terminalId,
            signPrivkey,
            signPubkey,
            30,
            5,
            false,
            true,
            0.4,
            1.25,
            Long.valueOf(7L)
        );
        BootstrapEnvironment.seedBootstrapLedger(ledger, "fusion");
    }

    public Transaction registerDevice(String deviceId, String devicePubkey) {
        if (ledger.getDeviceRegistry().containsKey(deviceId)) {
            throw new IllegalStateException("duplicate_device_id");
        }
        Transaction tx = ledger.createRegisterTx(deviceId, devicePubkey, assignAttributes(deviceId));
        if (!ledger.insertTransaction(tx)) {
            throw new IllegalStateException("local_insert_failed");
        }
        cloudStation.receiveBroadcast(tx.copy());
        return tx;
    }

    public Transaction submitDeviceData(String deviceId, Map<String, Object> dataPayload, String deviceSignature, double requestTs) {
        String regTxId = ledger.getDeviceRegistry().get(deviceId);
        if (regTxId == null) {
            throw new IllegalStateException("device_not_registered");
        }
        Transaction regTx = ledger.getTxIndex().get(regTxId);
        if (regTx == null || regTx.getDevicePubkey() == null) {
            throw new IllegalStateException("device_certificate_unavailable");
        }
        if (!"confirmed".equals(regTx.getStatus())) {
            throw new IllegalStateException("device_identity_not_confirmed");
        }

        Map<String, Object> authBody = new LinkedHashMap<String, Object>();
        authBody.put("device_id", deviceId);
        authBody.put("data_payload", dataPayload);
        authBody.put("request_ts", Double.valueOf(requestTs));
        if (!CryptoUtils.secpVerify(regTx.getDevicePubkey(), authBody, deviceSignature)) {
            throw new IllegalStateException("device_signature_invalid");
        }

        Map<String, Object> businessPayload = new LinkedHashMap<String, Object>();
        businessPayload.put("device_id", deviceId);
        businessPayload.put("auth_ref_tx_id", regTx.getTxId());
        businessPayload.put("data_payload", dataPayload);
        businessPayload.put("request_ts", Double.valueOf(requestTs));

        Transaction tx = ledger.createBusinessTx(businessPayload, deviceId, regTx.getTxId());
        if (!ledger.insertTransaction(tx)) {
            throw new IllegalStateException("device_business_insert_failed");
        }
        cloudStation.receiveBroadcast(tx.copy());
        return tx;
    }

    public void receiveBroadcast(Transaction tx) {
        ledger.insertTransaction(tx);
    }

    public void applyConfirmation(LifecycleAction action) {
        if ("confirm_register".equals(action.getAction())) {
            ledger.confirmRegister(action.getTxId());
            return;
        }
        if ("confirm_business".equals(action.getAction())) {
            ledger.confirmBusiness(action.getTxId());
            return;
        }
        if ("soft_delete_business".equals(action.getAction())) {
            ledger.softDeleteBusiness(action.getTxId());
            ledger.applyLocalPruning();
        }
    }

    public Map<String, Object> assignAttributes(String deviceId) {
        Map<String, Object> attributes = new LinkedHashMap<String, Object>();
        attributes.put("role", "edge_device");
        attributes.put("access_domain", terminalId);
        attributes.put("trust_level", "normal");
        attributes.put("device_group", deviceId.substring(Math.max(0, deviceId.length() - 4)));
        return attributes;
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

    public DagLedger getLedger() {
        return ledger;
    }
}



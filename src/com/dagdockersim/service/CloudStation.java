package com.dagdockersim.service;

import com.dagdockersim.bootstrap.BootstrapEnvironment;
import com.dagdockersim.crypto.CryptoUtils;
import com.dagdockersim.crypto.KeyPairStrings;
import com.dagdockersim.ledger.DagLedger;
import com.dagdockersim.model.LifecycleAction;
import com.dagdockersim.model.Transaction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CloudStation {
    private final DagLedger ledger;
    private final Map<String, Map<String, Object>> archive = new LinkedHashMap<String, Map<String, Object>>();
    private final List<FusionTerminal> attachedFusions = new ArrayList<FusionTerminal>();

    public CloudStation(int regConfirmThreshold, int dataConfirmThreshold, boolean preserveFullTransactions) {
        KeyPairStrings signer = CryptoUtils.generateSecp256k1KeyPair();
        this.ledger = new DagLedger(
            "cloud",
            signer.getPrivateKey(),
            signer.getPublicKey(),
            regConfirmThreshold,
            dataConfirmThreshold,
            false,
            preserveFullTransactions,
            0.4,
            1.25,
            Long.valueOf(7L)
        );
        for (Transaction tx : BootstrapEnvironment.seedBootstrapLedger(ledger, "cloud")) {
            archive.put(tx.getTxId(), tx.toMap());
        }
    }

    public void attachFusion(FusionTerminal fusion) {
        attachedFusions.add(fusion);
    }

    public BroadcastResult receiveBroadcast(Transaction tx) {
        archive.put(tx.getTxId(), tx.toMap());
        DagLedger.InsertResult insertResult = ledger.insertTransactionWithCandidates(tx.copy());

        List<String> forwardedTo = new ArrayList<String>();
        for (FusionTerminal fusion : attachedFusions) {
            if (!fusion.getTerminalId().equals(tx.getSourceTerminalId())) {
                fusion.receiveBroadcast(tx.copy());
                forwardedTo.add(fusion.getTerminalId());
            }
        }

        List<LifecycleAction> actions = insertResult.isInserted()
            ? ledger.applyCloudLifecycle(insertResult.getCandidates())
            : new ArrayList<LifecycleAction>();

        for (LifecycleAction action : actions) {
            for (FusionTerminal fusion : attachedFusions) {
                fusion.applyConfirmation(action);
            }
        }

        return new BroadcastResult(insertResult.isInserted(), forwardedTo, actions);
    }

    public DagLedger getLedger() {
        return ledger;
    }

    public Map<String, Map<String, Object>> getArchive() {
        return archive;
    }

    public static class BroadcastResult {
        private final boolean inserted;
        private final List<String> forwardedTo;
        private final List<LifecycleAction> actions;

        public BroadcastResult(boolean inserted, List<String> forwardedTo, List<LifecycleAction> actions) {
            this.inserted = inserted;
            this.forwardedTo = forwardedTo;
            this.actions = actions;
        }

        public boolean isInserted() {
            return inserted;
        }

        public List<String> getForwardedTo() {
            return forwardedTo;
        }

        public List<LifecycleAction> getActions() {
            return actions;
        }
    }
}

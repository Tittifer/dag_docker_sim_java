package com.dagdockersim.core.cloud;

import com.dagdockersim.core.bootstrap.BootstrapEnvironment;
import com.dagdockersim.core.crypto.CryptoUtils;
import com.dagdockersim.core.crypto.KeyPairStrings;
import com.dagdockersim.core.ledger.DagLedger;
import com.dagdockersim.model.domain.LifecycleAction;
import com.dagdockersim.model.domain.Transaction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CloudStation {
    private final DagLedger ledger;
    private final Map<String, Map<String, Object>> archive = new LinkedHashMap<String, Map<String, Object>>();

    public CloudStation(int regConfirmThreshold, int dataConfirmThreshold, boolean preserveFullTransactions) {
        this(regConfirmThreshold, dataConfirmThreshold, preserveFullTransactions, true);
    }

    public CloudStation(
        int regConfirmThreshold,
        int dataConfirmThreshold,
        boolean preserveFullTransactions,
        boolean seedBootstrap
    ) {
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
        if (seedBootstrap) {
            for (Transaction tx : BootstrapEnvironment.seedBootstrapLedger(ledger, "cloud")) {
                archive.put(tx.getTxId(), tx.toMap());
            }
        }
    }

    public CloudBroadcastOutcome receiveBroadcast(Transaction tx) {
        archive.put(tx.getTxId(), tx.toMap());
        DagLedger.InsertResult insertResult = ledger.insertTransactionWithCandidates(tx.copy());

        List<LifecycleAction> actions = insertResult.isInserted()
            ? ledger.applyCloudLifecycle(insertResult.getCandidates())
            : new ArrayList<LifecycleAction>();
        return new CloudBroadcastOutcome(insertResult.isInserted(), actions);
    }

    public DagLedger getLedger() {
        return ledger;
    }

    public Map<String, Map<String, Object>> getArchive() {
        return archive;
    }

    public void archiveTransaction(Transaction transaction) {
        archive.put(transaction.getTxId(), transaction.toMap());
    }

    public static class CloudBroadcastOutcome {
        private final boolean inserted;
        private final List<LifecycleAction> actions;

        public CloudBroadcastOutcome(boolean inserted, List<LifecycleAction> actions) {
            this.inserted = inserted;
            this.actions = actions;
        }

        public boolean isInserted() {
            return inserted;
        }

        public List<LifecycleAction> getActions() {
            return actions;
        }
    }
}


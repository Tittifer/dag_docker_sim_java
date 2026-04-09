package com.dagdockersim.cloud;

import com.dagdockersim.fusion.FusionTerminal;
import com.dagdockersim.shared.bootstrap.BootstrapEnvironment;
import com.dagdockersim.shared.crypto.CryptoUtils;
import com.dagdockersim.shared.crypto.KeyPairStrings;
import com.dagdockersim.shared.ledger.DagLedger;
import com.dagdockersim.shared.model.LifecycleAction;
import com.dagdockersim.shared.model.Transaction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CloudStation {
    private final DagLedger ledger;
    private final Map<String, Map<String, Object>> archive = new LinkedHashMap<String, Map<String, Object>>();
    private final List<FusionTerminal> attachedFusions = new ArrayList<FusionTerminal>();

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

    public void attachFusion(FusionTerminal fusion) {
        attachedFusions.add(fusion);
    }

    public void receiveBroadcast(Transaction tx) {
        archive.put(tx.getTxId(), tx.toMap());
        DagLedger.InsertResult insertResult = ledger.insertTransactionWithCandidates(tx.copy());

        for (FusionTerminal fusion : attachedFusions) {
            if (!fusion.getTerminalId().equals(tx.getSourceTerminalId())) {
                fusion.receiveBroadcast(tx.copy());
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
}


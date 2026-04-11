package com.dagdockersim.model.domain;

public class LifecycleAction {
    private final String action;
    private final String txId;

    public LifecycleAction(String action, String txId) {
        this.action = action;
        this.txId = txId;
    }

    public String getAction() {
        return action;
    }

    public String getTxId() {
        return txId;
    }

    @Override
    public String toString() {
        return "{action=" + action + ", tx_id=" + txId + "}";
    }
}


package com.dagdockersim.shared.model;

import com.dagdockersim.shared.util.JsonCanonicalizer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Transaction {
    private String txId;
    private String txType;
    private String sourceTerminalId;
    private String sourceTerminalPubkey;
    private List<String> parentIds;
    private double timestamp;
    private int createdRound;
    private String payloadHash;
    private String terminalSignature;
    private String deviceId;
    private String devicePubkey;
    private String targetTerminalId;
    private Map<String, Object> attributes;
    private Map<String, Object> payload;
    private int cumulativeWeight;
    private String status;
    private Map<String, Object> tombstone;
    private Double confirmedAt;
    private Double softPruneNotifiedAt;
    private Double softPrunedAt;
    private Double hardPrunedAt;

    public Transaction() {
        this.parentIds = new ArrayList<String>();
        this.cumulativeWeight = 1;
        this.status = "pending";
    }

    public Transaction copy() {
        Transaction copy = new Transaction();
        copy.txId = this.txId;
        copy.txType = this.txType;
        copy.sourceTerminalId = this.sourceTerminalId;
        copy.sourceTerminalPubkey = this.sourceTerminalPubkey;
        copy.parentIds = new ArrayList<String>(this.parentIds);
        copy.timestamp = this.timestamp;
        copy.createdRound = this.createdRound;
        copy.payloadHash = this.payloadHash;
        copy.terminalSignature = this.terminalSignature;
        copy.deviceId = this.deviceId;
        copy.devicePubkey = this.devicePubkey;
        copy.targetTerminalId = this.targetTerminalId;
        copy.attributes = mapCopy(this.attributes);
        copy.payload = mapCopy(this.payload);
        copy.cumulativeWeight = this.cumulativeWeight;
        copy.status = this.status;
        copy.tombstone = mapCopy(this.tombstone);
        copy.confirmedAt = this.confirmedAt;
        copy.softPruneNotifiedAt = this.softPruneNotifiedAt;
        copy.softPrunedAt = this.softPrunedAt;
        copy.hardPrunedAt = this.hardPrunedAt;
        return copy;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("tx_id", txId);
        data.put("tx_type", txType);
        data.put("source_terminal_id", sourceTerminalId);
        data.put("source_terminal_pubkey", sourceTerminalPubkey);
        data.put("parent_ids", new ArrayList<String>(parentIds));
        data.put("timestamp", timestamp);
        data.put("created_round", createdRound);
        data.put("payload_hash", payloadHash);
        data.put("terminal_signature", terminalSignature);
        data.put("device_id", deviceId);
        data.put("device_pubkey", devicePubkey);
        data.put("target_terminal_id", targetTerminalId);
        data.put("attributes", mapCopy(attributes));
        data.put("payload", mapCopy(payload));
        data.put("cumulative_weight", cumulativeWeight);
        data.put("status", status);
        data.put("tombstone", mapCopy(tombstone));
        data.put("confirmed_at", confirmedAt);
        data.put("soft_prune_notified_at", softPruneNotifiedAt);
        data.put("soft_pruned_at", softPrunedAt);
        data.put("hard_pruned_at", hardPrunedAt);
        return data;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapCopy(Map<String, Object> source) {
        if (source == null) {
            return null;
        }
        return (Map<String, Object>) JsonCanonicalizer.deepCopy(source);
    }

    public String getTxId() {
        return txId;
    }

    public void setTxId(String txId) {
        this.txId = txId;
    }

    public String getTxType() {
        return txType;
    }

    public void setTxType(String txType) {
        this.txType = txType;
    }

    public String getSourceTerminalId() {
        return sourceTerminalId;
    }

    public void setSourceTerminalId(String sourceTerminalId) {
        this.sourceTerminalId = sourceTerminalId;
    }

    public String getSourceTerminalPubkey() {
        return sourceTerminalPubkey;
    }

    public void setSourceTerminalPubkey(String sourceTerminalPubkey) {
        this.sourceTerminalPubkey = sourceTerminalPubkey;
    }

    public List<String> getParentIds() {
        return parentIds;
    }

    public void setParentIds(List<String> parentIds) {
        this.parentIds = parentIds;
    }

    public double getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(double timestamp) {
        this.timestamp = timestamp;
    }

    public int getCreatedRound() {
        return createdRound;
    }

    public void setCreatedRound(int createdRound) {
        this.createdRound = createdRound;
    }

    public String getPayloadHash() {
        return payloadHash;
    }

    public void setPayloadHash(String payloadHash) {
        this.payloadHash = payloadHash;
    }

    public String getTerminalSignature() {
        return terminalSignature;
    }

    public void setTerminalSignature(String terminalSignature) {
        this.terminalSignature = terminalSignature;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDevicePubkey() {
        return devicePubkey;
    }

    public void setDevicePubkey(String devicePubkey) {
        this.devicePubkey = devicePubkey;
    }

    public String getTargetTerminalId() {
        return targetTerminalId;
    }

    public void setTargetTerminalId(String targetTerminalId) {
        this.targetTerminalId = targetTerminalId;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public int getCumulativeWeight() {
        return cumulativeWeight;
    }

    public void setCumulativeWeight(int cumulativeWeight) {
        this.cumulativeWeight = cumulativeWeight;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, Object> getTombstone() {
        return tombstone;
    }

    public void setTombstone(Map<String, Object> tombstone) {
        this.tombstone = tombstone;
    }

    public Double getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(Double confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public Double getSoftPruneNotifiedAt() {
        return softPruneNotifiedAt;
    }

    public void setSoftPruneNotifiedAt(Double softPruneNotifiedAt) {
        this.softPruneNotifiedAt = softPruneNotifiedAt;
    }

    public Double getSoftPrunedAt() {
        return softPrunedAt;
    }

    public void setSoftPrunedAt(Double softPrunedAt) {
        this.softPrunedAt = softPrunedAt;
    }

    public Double getHardPrunedAt() {
        return hardPrunedAt;
    }

    public void setHardPrunedAt(Double hardPrunedAt) {
        this.hardPrunedAt = hardPrunedAt;
    }
}


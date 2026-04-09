package com.dagdockersim.simulation.infrastructure.persistence.ledger.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(
    name = "ledger_transactions",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_terminal_tx", columnNames = {"terminal_id", "tx_id"})
    }
)
public class LedgerTransactionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "terminal_id", nullable = false, length = 64)
    private String terminalId;

    @Column(name = "tx_id", nullable = false, length = 128)
    private String txId;

    @Column(name = "tx_type", nullable = false, length = 32)
    private String txType;

    @Column(name = "source_terminal_id", nullable = false, length = 64)
    private String sourceTerminalId;

    @Lob
    @Column(name = "source_terminal_pubkey", columnDefinition = "LONGTEXT")
    private String sourceTerminalPubkey;

    @Lob
    @Column(name = "parent_ids_json", columnDefinition = "LONGTEXT")
    private String parentIdsJson;

    @Column(name = "timestamp_value", nullable = false)
    private Double timestamp;

    @Column(name = "created_round_value", nullable = false)
    private Integer createdRound;

    @Column(name = "payload_hash", length = 128)
    private String payloadHash;

    @Lob
    @Column(name = "terminal_signature", columnDefinition = "LONGTEXT")
    private String terminalSignature;

    @Column(name = "device_id", length = 128)
    private String deviceId;

    @Lob
    @Column(name = "device_pubkey", columnDefinition = "LONGTEXT")
    private String devicePubkey;

    @Column(name = "target_terminal_id", length = 64)
    private String targetTerminalId;

    @Lob
    @Column(name = "attributes_json", columnDefinition = "LONGTEXT")
    private String attributesJson;

    @Lob
    @Column(name = "payload_json", columnDefinition = "LONGTEXT")
    private String payloadJson;

    @Column(name = "cumulative_weight_value", nullable = false)
    private Integer cumulativeWeight;

    @Column(name = "status_value", nullable = false, length = 32)
    private String status;

    @Lob
    @Column(name = "tombstone_json", columnDefinition = "LONGTEXT")
    private String tombstoneJson;

    @Column(name = "confirmed_at")
    private Double confirmedAt;

    @Column(name = "soft_prune_notified_at")
    private Double softPruneNotifiedAt;

    @Column(name = "soft_pruned_at")
    private Double softPrunedAt;

    @Column(name = "hard_pruned_at")
    private Double hardPrunedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTerminalId() {
        return terminalId;
    }

    public void setTerminalId(String terminalId) {
        this.terminalId = terminalId;
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

    public String getParentIdsJson() {
        return parentIdsJson;
    }

    public void setParentIdsJson(String parentIdsJson) {
        this.parentIdsJson = parentIdsJson;
    }

    public Double getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Double timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getCreatedRound() {
        return createdRound;
    }

    public void setCreatedRound(Integer createdRound) {
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

    public String getAttributesJson() {
        return attributesJson;
    }

    public void setAttributesJson(String attributesJson) {
        this.attributesJson = attributesJson;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public Integer getCumulativeWeight() {
        return cumulativeWeight;
    }

    public void setCumulativeWeight(Integer cumulativeWeight) {
        this.cumulativeWeight = cumulativeWeight;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTombstoneJson() {
        return tombstoneJson;
    }

    public void setTombstoneJson(String tombstoneJson) {
        this.tombstoneJson = tombstoneJson;
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

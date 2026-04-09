package com.dagdockersim.simulation.infrastructure.persistence.ledger;

import com.dagdockersim.shared.model.Transaction;
import com.dagdockersim.simulation.infrastructure.persistence.ledger.entity.LedgerTransactionEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class TransactionPersistenceMapper {
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<List<String>>() { };
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String, Object>>() { };

    private final ObjectMapper objectMapper;

    public TransactionPersistenceMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public LedgerTransactionEntity toEntity(String terminalId, Transaction transaction) {
        LedgerTransactionEntity entity = new LedgerTransactionEntity();
        entity.setTerminalId(terminalId);
        entity.setTxId(transaction.getTxId());
        entity.setTxType(transaction.getTxType());
        entity.setSourceTerminalId(transaction.getSourceTerminalId());
        entity.setSourceTerminalPubkey(transaction.getSourceTerminalPubkey());
        entity.setParentIdsJson(writeJson(transaction.getParentIds()));
        entity.setTimestamp(Double.valueOf(transaction.getTimestamp()));
        entity.setCreatedRound(Integer.valueOf(transaction.getCreatedRound()));
        entity.setPayloadHash(transaction.getPayloadHash());
        entity.setTerminalSignature(transaction.getTerminalSignature());
        entity.setDeviceId(transaction.getDeviceId());
        entity.setDevicePubkey(transaction.getDevicePubkey());
        entity.setTargetTerminalId(transaction.getTargetTerminalId());
        entity.setAttributesJson(writeJson(transaction.getAttributes()));
        entity.setPayloadJson(writeJson(transaction.getPayload()));
        entity.setCumulativeWeight(Integer.valueOf(transaction.getCumulativeWeight()));
        entity.setStatus(transaction.getStatus());
        entity.setTombstoneJson(writeJson(transaction.getTombstone()));
        entity.setConfirmedAt(transaction.getConfirmedAt());
        entity.setSoftPruneNotifiedAt(transaction.getSoftPruneNotifiedAt());
        entity.setSoftPrunedAt(transaction.getSoftPrunedAt());
        entity.setHardPrunedAt(transaction.getHardPrunedAt());
        return entity;
    }

    public Transaction toTransaction(LedgerTransactionEntity entity) {
        Transaction transaction = new Transaction();
        transaction.setTxId(entity.getTxId());
        transaction.setTxType(entity.getTxType());
        transaction.setSourceTerminalId(entity.getSourceTerminalId());
        transaction.setSourceTerminalPubkey(entity.getSourceTerminalPubkey());
        transaction.setParentIds(readStringList(entity.getParentIdsJson()));
        transaction.setTimestamp(entity.getTimestamp() == null ? 0.0 : entity.getTimestamp().doubleValue());
        transaction.setCreatedRound(entity.getCreatedRound() == null ? 0 : entity.getCreatedRound().intValue());
        transaction.setPayloadHash(entity.getPayloadHash());
        transaction.setTerminalSignature(entity.getTerminalSignature());
        transaction.setDeviceId(entity.getDeviceId());
        transaction.setDevicePubkey(entity.getDevicePubkey());
        transaction.setTargetTerminalId(entity.getTargetTerminalId());
        transaction.setAttributes(readMap(entity.getAttributesJson()));
        transaction.setPayload(readMap(entity.getPayloadJson()));
        transaction.setCumulativeWeight(entity.getCumulativeWeight() == null ? 0 : entity.getCumulativeWeight().intValue());
        transaction.setStatus(entity.getStatus());
        transaction.setTombstone(readMap(entity.getTombstoneJson()));
        transaction.setConfirmedAt(entity.getConfirmedAt());
        transaction.setSoftPruneNotifiedAt(entity.getSoftPruneNotifiedAt());
        transaction.setSoftPrunedAt(entity.getSoftPrunedAt());
        transaction.setHardPrunedAt(entity.getHardPrunedAt());
        return transaction;
    }

    private String writeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("failed_to_serialize_transaction_field", exception);
        }
    }

    private List<String> readStringList(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<String>();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST_TYPE);
        } catch (IOException exception) {
            throw new IllegalStateException("failed_to_deserialize_string_list", exception);
        }
    }

    private Map<String, Object> readMap(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (IOException exception) {
            throw new IllegalStateException("failed_to_deserialize_map", exception);
        }
    }
}

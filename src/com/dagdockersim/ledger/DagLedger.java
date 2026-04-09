package com.dagdockersim.ledger;

import com.dagdockersim.crypto.CryptoUtils;
import com.dagdockersim.model.LifecycleAction;
import com.dagdockersim.model.Transaction;
import com.dagdockersim.util.JsonCanonicalizer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

public class DagLedger {
    public static final List<String> GLOBAL_GENESIS_IDS =
        Collections.unmodifiableList(Arrays.asList("GENESIS_GLOBAL_A", "GENESIS_GLOBAL_B"));

    private final String terminalId;
    private final String terminalSignPrivkey;
    private final String terminalSignPubkey;
    private final int regConfirmThreshold;
    private final int dataConfirmThreshold;
    private final boolean manageLifecycle;
    private final boolean preserveFullTransactions;
    private final double mcmcAlpha;
    private final double identityBiasGamma;
    private final Random random;
    private final ReentrantLock lock = new ReentrantLock(true);

    private final Map<String, Transaction> txIndex = new LinkedHashMap<String, Transaction>();
    private final Map<String, Integer> weightIndex = new LinkedHashMap<String, Integer>();
    private final Map<String, String> deviceRegistry = new LinkedHashMap<String, String>();
    private final Set<String> tipSet = new LinkedHashSet<String>();
    private final Map<String, Set<String>> parentsByTx = new LinkedHashMap<String, Set<String>>();
    private final Map<String, Set<String>> childrenByTx = new LinkedHashMap<String, Set<String>>();
    private int round = 0;

    public DagLedger(
        String terminalId,
        String terminalSignPrivkey,
        String terminalSignPubkey,
        int regConfirmThreshold,
        int dataConfirmThreshold,
        boolean manageLifecycle,
        boolean preserveFullTransactions,
        double mcmcAlpha,
        double identityBiasGamma,
        Long randomSeed
    ) {
        this.terminalId = terminalId;
        this.terminalSignPrivkey = terminalSignPrivkey;
        this.terminalSignPubkey = terminalSignPubkey;
        this.regConfirmThreshold = regConfirmThreshold;
        this.dataConfirmThreshold = dataConfirmThreshold;
        this.manageLifecycle = manageLifecycle;
        this.preserveFullTransactions = preserveFullTransactions;
        this.mcmcAlpha = mcmcAlpha;
        this.identityBiasGamma = identityBiasGamma;
        this.random = randomSeed == null ? new Random() : new Random(randomSeed.longValue());
        initGenesis();
    }

    public DagLedger(String terminalId, String terminalSignPrivkey, String terminalSignPubkey) {
        this(terminalId, terminalSignPrivkey, terminalSignPubkey, 30, 5, false, false, 0.4, 1.25, null);
    }

    public Transaction createRegisterTx(String deviceId, String devicePubkey, Map<String, Object> attributes) {
        List<String> parents = ensureParentCount(
            selectRegistrationParents(),
            2,
            setOf("genesis", "register"),
            Collections.<String>emptySet(),
            Collections.<String>emptySet(),
            setOf("pending", "confirmed"),
            true
        );
        lock.lock();
        try {
            round += 1;
            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("tx_type", "register");
            payload.put("device_id", deviceId);
            payload.put("device_pubkey", devicePubkey);
            payload.put("target_terminal_id", terminalId);
            payload.put("attributes", mapCopy(attributes));
            payload.put("parent_ids", new ArrayList<String>(parents));
            payload.put("source_terminal_id", terminalId);
            payload.put("source_terminal_pubkey", terminalSignPubkey);
            payload.put("round", Integer.valueOf(round));
            payload.put("timestamp", Double.valueOf(now()));
            return buildSignedTx(
                "register",
                payload,
                parents,
                deviceId,
                devicePubkey,
                terminalId,
                mapCopy(attributes)
            );
        } finally {
            lock.unlock();
        }
    }

    public List<String> selectRegistrationParents() {
        List<String> parents = selectConventionalParents("register", 2, Collections.<String>emptySet());
        return ensureParentCount(
            parents,
            2,
            setOf("genesis", "register"),
            Collections.<String>emptySet(),
            Collections.<String>emptySet(),
            setOf("pending", "confirmed"),
            true
        );
    }

    public Transaction createBusinessTx(
        Map<String, Object> businessPayload,
        String originDeviceId,
        String identityAnchorTxId
    ) {
        Map<String, Object> payloadBody = mapCopy(businessPayload);
        String deviceId = originDeviceId != null ? originDeviceId : stringValue(payloadBody.get("device_id"));
        if (deviceId == null || deviceId.isEmpty()) {
            throw new IllegalArgumentException("device_id_required");
        }
        String requestedAnchor = identityAnchorTxId;
        if (requestedAnchor == null) {
            requestedAnchor = stringValue(payloadBody.get("auth_ref_tx_id"));
            if (requestedAnchor == null) {
                requestedAnchor = stringValue(payloadBody.get("identity_ref_tx_id"));
            }
        }
        String resolvedAnchor = resolveIdentityAnchor(deviceId, requestedAnchor, setOf("confirmed"));
        if (resolvedAnchor == null) {
            throw new IllegalArgumentException("device_identity_anchor_unavailable");
        }
        payloadBody.put("auth_ref_tx_id", resolvedAnchor);
        List<String> parents = selectBusinessParents(deviceId, resolvedAnchor);
        lock.lock();
        try {
            round += 1;
            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("tx_type", "business");
            payload.put("business_payload", payloadBody);
            payload.put("parent_ids", new ArrayList<String>(parents));
            payload.put("source_terminal_id", terminalId);
            payload.put("source_terminal_pubkey", terminalSignPubkey);
            payload.put("round", Integer.valueOf(round));
            payload.put("timestamp", Double.valueOf(now()));
            payload.put("device_id", deviceId);
            payload.put("identity_ref_tx_id", resolvedAnchor);
            return buildSignedTx("business", payload, parents, deviceId, null, null, null);
        } finally {
            lock.unlock();
        }
    }

    public boolean verifyTransaction(Transaction tx) {
        if ("genesis".equals(tx.getTxType())) {
            return GLOBAL_GENESIS_IDS.contains(tx.getTxId());
        }
        if ("business".equals(tx.getTxType()) && "soft_deleted".equals(tx.getStatus())) {
            Map<String, Object> tombstone = tx.getTombstone();
            return tombstone != null
                && Objects.equals(tombstone.get("tx_hash"), tx.getTxId())
                && Objects.equals(tombstone.get("tx_type"), "business")
                && Objects.equals(tombstone.get("parents"), new ArrayList<String>(tx.getParentIds()))
                && Objects.equals(tombstone.get("state"), "soft_deleted");
        }
        if (tx.getPayload() == null) {
            return false;
        }
        if (!Objects.equals(CryptoUtils.sha256Hex(tx.getPayload()), tx.getPayloadHash())) {
            return false;
        }
        if (!Objects.equals(CryptoUtils.sha256Hex(tx.getPayload()), tx.getTxId())) {
            return false;
        }
        if (!Objects.equals(tx.getPayload().get("tx_type"), tx.getTxType())) {
            return false;
        }
        if (!Objects.equals(listValue(tx.getPayload().get("parent_ids")), tx.getParentIds())) {
            return false;
        }
        if (!Objects.equals(tx.getPayload().get("source_terminal_id"), tx.getSourceTerminalId())) {
            return false;
        }
        if (!Objects.equals(tx.getPayload().get("source_terminal_pubkey"), tx.getSourceTerminalPubkey())) {
            return false;
        }
        if ("register".equals(tx.getTxType())) {
            if (!tx.getPayload().keySet().containsAll(setOf("device_id", "device_pubkey", "target_terminal_id", "attributes"))) {
                return false;
            }
            if (!Objects.equals(tx.getPayload().get("device_id"), tx.getDeviceId())) {
                return false;
            }
            if (!Objects.equals(tx.getPayload().get("device_pubkey"), tx.getDevicePubkey())) {
                return false;
            }
            if (!Objects.equals(tx.getPayload().get("target_terminal_id"), tx.getTargetTerminalId())) {
                return false;
            }
        } else if ("business".equals(tx.getTxType())) {
            Object businessPayload = tx.getPayload().get("business_payload");
            if (!(businessPayload instanceof Map<?, ?>)) {
                return false;
            }
            if (!Objects.equals(tx.getPayload().get("device_id"), tx.getDeviceId())) {
                return false;
            }
            String identityRef = stringValue(tx.getPayload().get("identity_ref_tx_id"));
            String authRef = stringValue(((Map<?, ?>) businessPayload).get("auth_ref_tx_id"));
            if (tx.getDeviceId() == null || identityRef == null || authRef == null) {
                return false;
            }
            if (!Objects.equals(identityRef, authRef)) {
                return false;
            }
        } else {
            return false;
        }
        return CryptoUtils.secpVerify(tx.getSourceTerminalPubkey(), tx.getPayload(), tx.getTerminalSignature());
    }

    public boolean hasConflict(Transaction tx) {
        return "register".equals(tx.getTxType()) && tx.getDeviceId() != null && deviceRegistry.containsKey(tx.getDeviceId());
    }

    public InsertResult insertTransactionWithCandidates(Transaction tx) {
        lock.lock();
        try {
            if (txIndex.containsKey(tx.getTxId())) {
                return new InsertResult(false, Collections.<String>emptyList());
            }
            if (!verifyTransaction(tx) || hasConflict(tx)) {
                return new InsertResult(false, Collections.<String>emptyList());
            }
            for (String parentId : tx.getParentIds()) {
                if (!txIndex.containsKey(parentId)) {
                    return new InsertResult(false, Collections.<String>emptyList());
                }
            }
            if (!validateParentStructure(tx)) {
                return new InsertResult(false, Collections.<String>emptyList());
            }
            if ("confirmed".equals(tx.getStatus()) && tx.getConfirmedAt() == null) {
                tx.setConfirmedAt(Double.valueOf(tx.getTimestamp()));
            }
            txIndex.put(tx.getTxId(), tx);
            weightIndex.put(tx.getTxId(), Integer.valueOf(tx.getCumulativeWeight()));
            parentsByTx.put(tx.getTxId(), new LinkedHashSet<String>(tx.getParentIds()));
            if (!childrenByTx.containsKey(tx.getTxId())) {
                childrenByTx.put(tx.getTxId(), new LinkedHashSet<String>());
            }
            for (String parentId : tx.getParentIds()) {
                tipSet.remove(parentId);
                if (!childrenByTx.containsKey(parentId)) {
                    childrenByTx.put(parentId, new LinkedHashSet<String>());
                }
                childrenByTx.get(parentId).add(tx.getTxId());
            }
            tipSet.add(tx.getTxId());
            if ("register".equals(tx.getTxType()) && tx.getDeviceId() != null) {
                deviceRegistry.put(tx.getDeviceId(), tx.getTxId());
            }
            List<String> updatedIds = increaseAncestorWeights(tx.getTxId());
            List<String> candidates = new ArrayList<String>();
            candidates.add(tx.getTxId());
            candidates.addAll(updatedIds);
            if (manageLifecycle) {
                applyCloudLifecycle(candidates);
            }
            return new InsertResult(true, candidates);
        } finally {
            lock.unlock();
        }
    }

    public boolean insertTransaction(Transaction tx) {
        return insertTransactionWithCandidates(tx).isInserted();
    }

    public Integer transactionWeight(String txId) {
        lock.lock();
        try {
            Integer known = weightIndex.get(txId);
            if (known != null) {
                return known;
            }
            Transaction tx = txIndex.get(txId);
            return tx == null ? null : Integer.valueOf(tx.getCumulativeWeight());
        } finally {
            lock.unlock();
        }
    }

    public boolean confirmRegister(String txId) {
        lock.lock();
        try {
            Transaction tx = txIndex.get(txId);
            if (tx == null || !"register".equals(tx.getTxType())) {
                return false;
            }
            tx.setStatus("confirmed");
            if (tx.getConfirmedAt() == null) {
                tx.setConfirmedAt(Double.valueOf(now()));
            }
            return true;
        } finally {
            lock.unlock();
        }
    }

    public boolean confirmBusiness(String txId) {
        lock.lock();
        try {
            Transaction tx = txIndex.get(txId);
            if (tx == null || !"business".equals(tx.getTxType()) || "hard_deleted".equals(tx.getStatus())) {
                return false;
            }
            tx.setStatus("confirmed");
            if (tx.getConfirmedAt() == null) {
                tx.setConfirmedAt(Double.valueOf(now()));
            }
            return true;
        } finally {
            lock.unlock();
        }
    }

    public boolean softDeleteBusiness(String txId) {
        lock.lock();
        try {
            Transaction tx = txIndex.get(txId);
            if (tx == null || !"business".equals(tx.getTxType()) || "hard_deleted".equals(tx.getStatus())) {
                return false;
            }
            if ("soft_deleted".equals(tx.getStatus())) {
                return true;
            }
            if (!"confirmed".equals(tx.getStatus())) {
                return false;
            }
            tx.setSoftPrunedAt(Double.valueOf(now()));
            if (preserveFullTransactions) {
                tx.setStatus("soft_deleted");
                return true;
            }
            Map<String, Object> tombstone = new LinkedHashMap<String, Object>();
            tombstone.put("tx_hash", tx.getTxId());
            tombstone.put("tx_type", tx.getTxType());
            tombstone.put("state", "soft_deleted");
            tombstone.put("parents", new ArrayList<String>(tx.getParentIds()));
            tombstone.put("ts_confirmed", tx.getConfirmedAt());
            tx.setTombstone(tombstone);
            tx.setPayload(mapCopy(tombstone));
            tx.setAttributes(null);
            tx.setDevicePubkey(null);
            tx.setStatus("soft_deleted");
            return true;
        } finally {
            lock.unlock();
        }
    }

    public boolean hardDeleteBusiness(String txId) {
        lock.lock();
        try {
            Transaction tx = txIndex.get(txId);
            if (tx == null || !"business".equals(tx.getTxType()) || !"soft_deleted".equals(tx.getStatus())) {
                return false;
            }
            tx.setHardPrunedAt(Double.valueOf(now()));
            if (preserveFullTransactions) {
                tx.setStatus("hard_deleted");
                tipSet.remove(txId);
                return true;
            }
            List<String> successors = new ArrayList<String>(childrenByTx(txId));
            for (String childId : successors) {
                Transaction child = txIndex.get(childId);
                if (child == null) {
                    continue;
                }
                List<String> newParents = new ArrayList<String>();
                for (String parentId : child.getParentIds()) {
                    if (!txId.equals(parentId)) {
                        newParents.add(parentId);
                    }
                }
                newParents = ensureParentCount(
                    newParents,
                    2,
                    null,
                    Collections.<String>emptySet(),
                    Collections.<String>emptySet(),
                    null,
                    false
                );
                child.setParentIds(newParents);
                if (child.getPayload() != null) {
                    child.getPayload().put("parent_ids", new ArrayList<String>(newParents));
                }
                for (String oldParent : new ArrayList<String>(parentsByTx(childId))) {
                    unlinkParent(oldParent, childId);
                }
                for (String newParent : newParents) {
                    linkParent(newParent, childId);
                }
            }
            tx.setPayload(null);
            tx.setAttributes(null);
            tx.setDevicePubkey(null);
            tx.setParentIds(new ArrayList<String>());
            tx.setStatus("hard_deleted");
            for (String parentId : new ArrayList<String>(parentsByTx(txId))) {
                unlinkParent(parentId, txId);
            }
            for (String childId : new ArrayList<String>(childrenByTx(txId))) {
                unlinkParent(txId, childId);
            }
            parentsByTx.remove(txId);
            childrenByTx.remove(txId);
            tipSet.remove(txId);
            txIndex.remove(txId);
            weightIndex.remove(txId);
            return true;
        } finally {
            lock.unlock();
        }
    }

    public List<LifecycleAction> applyLocalPruning() {
        lock.lock();
        try {
            List<LifecycleAction> actions = new ArrayList<LifecycleAction>();
            while (true) {
                boolean hardDeleted = false;
                for (Transaction tx : new ArrayList<Transaction>(txIndex.values())) {
                    if ("business".equals(tx.getTxType())
                        && "soft_deleted".equals(tx.getStatus())
                        && canHardDelete(tx.getTxId())) {
                        hardDeleteBusiness(tx.getTxId());
                        actions.add(new LifecycleAction("hard_delete_business", tx.getTxId()));
                        hardDeleted = true;
                        break;
                    }
                }
                if (!hardDeleted) {
                    break;
                }
            }
            return actions;
        } finally {
            lock.unlock();
        }
    }

    public List<LifecycleAction> applyCloudLifecycle() {
        return applyCloudLifecycle((Collection<String>) null);
    }

    public List<LifecycleAction> applyCloudLifecycle(Collection<String> candidateTxIds) {
        lock.lock();
        try {
            List<LifecycleAction> actions = new ArrayList<LifecycleAction>();
            List<String> candidateIds = candidateTxIds == null
                ? orderedTxIds(txIndex.keySet())
                : orderedTxIds(new LinkedHashSet<String>(candidateTxIds));
            for (String txId : candidateIds) {
                Transaction tx = txIndex.get(txId);
                if (tx == null) {
                    continue;
                }
                int weight = weightIndex.containsKey(txId) ? weightIndex.get(txId).intValue() : tx.getCumulativeWeight();
                if ("register".equals(tx.getTxType()) && "pending".equals(tx.getStatus()) && weight >= regConfirmThreshold) {
                    confirmRegister(txId);
                    actions.add(new LifecycleAction("confirm_register", txId));
                    tx = txIndex.get(txId);
                }
                if (tx == null || !"business".equals(tx.getTxType())) {
                    continue;
                }
                if ("pending".equals(tx.getStatus()) && weight >= dataConfirmThreshold) {
                    confirmBusiness(txId);
                    actions.add(new LifecycleAction("confirm_business", txId));
                    tx = txIndex.get(txId);
                }
                if (tx != null
                    && "confirmed".equals(tx.getStatus())
                    && weight >= dataConfirmThreshold
                    && tx.getSoftPruneNotifiedAt() == null) {
                    tx.setSoftPruneNotifiedAt(Double.valueOf(now()));
                    actions.add(new LifecycleAction("soft_delete_business", txId));
                }
            }
            return actions;
        } finally {
            lock.unlock();
        }
    }

    public Map<String, Object> summary() {
        lock.lock();
        try {
            Map<String, Integer> counts = new LinkedHashMap<String, Integer>();
            counts.put("pending", Integer.valueOf(0));
            counts.put("confirmed", Integer.valueOf(0));
            counts.put("soft_deleted", Integer.valueOf(0));
            counts.put("hard_deleted", Integer.valueOf(0));
            counts.put("genesis", Integer.valueOf(0));
            for (Transaction tx : txIndex.values()) {
                if ("genesis".equals(tx.getTxType())) {
                    counts.put("genesis", Integer.valueOf(counts.get("genesis").intValue() + 1));
                } else {
                    String status = tx.getStatus();
                    counts.put(status, Integer.valueOf(counts.containsKey(status) ? counts.get(status).intValue() + 1 : 1));
                }
            }
            Map<String, Object> result = new LinkedHashMap<String, Object>();
            result.put("terminal_id", terminalId);
            result.put("tx_total", Integer.valueOf(txIndex.size()));
            result.put("tip_count", Integer.valueOf(tipSet.size()));
            result.put("device_count", Integer.valueOf(deviceRegistry.size()));
            result.put("counts", counts);
            return result;
        } finally {
            lock.unlock();
        }
    }

    public List<String> selectBusinessParents(String originDeviceId, String identityAnchorTxId) {
        if (originDeviceId == null || originDeviceId.isEmpty()) {
            throw new IllegalArgumentException("device_id_required");
        }
        String anchorId = resolveIdentityAnchor(originDeviceId, identityAnchorTxId, setOf("confirmed"));
        if (anchorId == null) {
            throw new IllegalArgumentException("device_identity_anchor_unavailable");
        }
        List<String> conventional = selectConventionalParents("business", 2, setOf(anchorId));
        if (conventional.size() < 2) {
            conventional = ensureParentCount(
                conventional,
                2,
                null,
                setOf(anchorId),
                Collections.<String>emptySet(),
                null,
                false
            );
        }
        List<String> parents = new ArrayList<String>(conventional.subList(0, Math.min(2, conventional.size())));
        parents.add(anchorId);
        return parents;
    }

    public String selectRegisterAnchor(Set<String> excludeDeviceIds, Set<String> allowedStatuses) {
        List<String> anchors = selectRegisterAnchors(
            excludeDeviceIds,
            allowedStatuses == null ? setOf("pending", "confirmed") : allowedStatuses
        );
        return anchors.isEmpty() ? null : anchors.get(0);
    }

    private String resolveIdentityAnchor(String originDeviceId, String identityAnchorTxId, Set<String> allowedStatuses) {
        lock.lock();
        try {
            String candidateId = identityAnchorTxId;
            if (candidateId == null && originDeviceId != null) {
                candidateId = deviceRegistry.get(originDeviceId);
            }
            if (candidateId == null) {
                return null;
            }
            Transaction tx = txIndex.get(candidateId);
            if (tx == null || !"register".equals(tx.getTxType())) {
                return null;
            }
            if (originDeviceId != null && !Objects.equals(originDeviceId, tx.getDeviceId())) {
                return null;
            }
            if (allowedStatuses != null && !allowedStatuses.contains(tx.getStatus())) {
                return null;
            }
            return candidateId;
        } finally {
            lock.unlock();
        }
    }

    private String txFamily(Transaction tx) {
        if ("register".equals(tx.getTxType())) {
            return "reg";
        }
        if ("business".equals(tx.getTxType())) {
            return "data";
        }
        return "genesis";
    }

    private int nonGenesisTxCount() {
        int count = 0;
        for (Transaction tx : txIndex.values()) {
            if (!"genesis".equals(tx.getTxType())) {
                count++;
            }
        }
        return count;
    }

    private boolean genesisBootstrapOpen() {
        return nonGenesisTxCount() < 2;
    }

    private boolean hasOnlyGenesisPredecessors(String txId) {
        Set<String> predecessors = parentsByTx(txId);
        if (predecessors.isEmpty()) {
            return true;
        }
        for (String parentId : predecessors) {
            Transaction parent = txIndex.get(parentId);
            if (parent == null || !"genesis".equals(parent.getTxType())) {
                return false;
            }
        }
        return true;
    }

    private double compatibilityBias(String newTxType, Transaction candidateTx) {
        String candidateFamily = txFamily(candidateTx);
        if ("register".equals(newTxType) && "data".equals(candidateFamily)) {
            return Double.NEGATIVE_INFINITY;
        }
        if ("business".equals(newTxType) && "reg".equals(candidateFamily) && !"confirmed".equals(candidateTx.getStatus())) {
            return identityBiasGamma;
        }
        return 0.0;
    }

    private boolean isActiveLocalNode(Transaction tx) {
        return !"hard_deleted".equals(tx.getStatus());
    }

    private List<String> eligibleStartNodes(String newTxType) {
        List<String> startNodes = new ArrayList<String>();
        for (Map.Entry<String, Transaction> entry : txIndex.entrySet()) {
            String txId = entry.getKey();
            Transaction tx = entry.getValue();
            if (!isActiveLocalNode(tx)) {
                continue;
            }
            if ("genesis".equals(tx.getTxType())) {
                if (!genesisBootstrapOpen()) {
                    continue;
                }
            } else if (!hasOnlyGenesisPredecessors(txId) && !parentsByTx(txId).isEmpty()) {
                continue;
            }
            if ("register".equals(newTxType) && "data".equals(txFamily(tx))) {
                continue;
            }
            startNodes.add(txId);
        }
        return orderedTxIds(startNodes);
    }

    private List<WeightedEdge> weightedSuccessors(String currentId, String newTxType, Set<String> excludedParentIds) {
        List<WeightedEdge> weighted = new ArrayList<WeightedEdge>();
        Set<String> excluded = excludedParentIds == null ? Collections.<String>emptySet() : excludedParentIds;
        for (String successorId : childrenByTx(currentId)) {
            if (excluded.contains(successorId)) {
                continue;
            }
            Transaction successor = txIndex.get(successorId);
            if (successor == null || !isActiveLocalNode(successor) || "genesis".equals(successor.getTxType())) {
                continue;
            }
            double bias = compatibilityBias(newTxType, successor);
            if (Double.isInfinite(bias) && bias < 0) {
                continue;
            }
            double exponent = mcmcAlpha * Math.max(1, successor.getCumulativeWeight()) + bias;
            exponent = Math.max(Math.min(exponent, 700.0), -700.0);
            weighted.add(new WeightedEdge(successorId, Math.exp(exponent)));
        }
        return weighted;
    }

    private String runIbtMcmcWalk(String newTxType, Set<String> excludedParentIds) {
        List<String> startNodes = eligibleStartNodes(newTxType);
        if (startNodes.isEmpty()) {
            return null;
        }
        String currentId = startNodes.get(random.nextInt(startNodes.size()));
        while (true) {
            List<WeightedEdge> weightedSuccessors = weightedSuccessors(currentId, newTxType, excludedParentIds);
            if (weightedSuccessors.isEmpty()) {
                return excludedParentIds != null && excludedParentIds.contains(currentId) ? null : currentId;
            }
            currentId = pickWeighted(weightedSuccessors);
        }
    }

    private List<String> terminalCandidates(String newTxType, Set<String> excludedParentIds) {
        List<String> terminals = new ArrayList<String>();
        Set<String> excluded = excludedParentIds == null ? Collections.<String>emptySet() : excludedParentIds;
        for (Map.Entry<String, Transaction> entry : txIndex.entrySet()) {
            String txId = entry.getKey();
            Transaction tx = entry.getValue();
            if (excluded.contains(txId) || !isActiveLocalNode(tx)) {
                continue;
            }
            if ("genesis".equals(tx.getTxType()) && !genesisBootstrapOpen()) {
                continue;
            }
            if ("register".equals(newTxType) && "data".equals(txFamily(tx))) {
                continue;
            }
            if (weightedSuccessors(txId, newTxType, excludedParentIds).isEmpty()) {
                terminals.add(txId);
            }
        }
        Collections.sort(terminals, new Comparator<String>() {
            @Override
            public int compare(String left, String right) {
                Transaction leftTx = txIndex.get(left);
                Transaction rightTx = txIndex.get(right);
                int byTime = Double.compare(rightTx.getTimestamp(), leftTx.getTimestamp());
                if (byTime != 0) {
                    return byTime;
                }
                int byWeight = Integer.compare(rightTx.getCumulativeWeight(), leftTx.getCumulativeWeight());
                if (byWeight != 0) {
                    return byWeight;
                }
                return right.compareTo(left);
            }
        });
        return terminals;
    }

    private List<String> selectConventionalParents(String newTxType, int count, Set<String> excludedParentIds) {
        lock.lock();
        try {
            Set<String> excluded = excludedParentIds == null ? Collections.<String>emptySet() : excludedParentIds;
            List<String> chosen = new ArrayList<String>();
            int attempts = 0;
            int maxAttempts = Math.max(8, count * 6);
            while (chosen.size() < count && attempts < maxAttempts) {
                attempts++;
                Set<String> excludedNow = new LinkedHashSet<String>(excluded);
                excludedNow.addAll(chosen);
                String tipId = runIbtMcmcWalk(newTxType, excludedNow);
                if (tipId != null && !chosen.contains(tipId)) {
                    chosen.add(tipId);
                }
            }
            if (chosen.size() >= count) {
                return chosen;
            }
            Set<String> excludedNow = new LinkedHashSet<String>(excluded);
            excludedNow.addAll(chosen);
            for (String candidateId : terminalCandidates(newTxType, excludedNow)) {
                if (!chosen.contains(candidateId)) {
                    chosen.add(candidateId);
                    if (chosen.size() >= count) {
                        break;
                    }
                }
            }
            return chosen;
        } finally {
            lock.unlock();
        }
    }

    private List<String> selectRegisterAnchors(Set<String> excludeDeviceIds, Set<String> allowedStatuses) {
        lock.lock();
        try {
            Set<String> excluded = excludeDeviceIds == null ? Collections.<String>emptySet() : excludeDeviceIds;
            Set<String> statuses = allowedStatuses == null ? setOf("pending", "confirmed") : allowedStatuses;
            List<String> anchors = new ArrayList<String>();
            for (Map.Entry<String, Transaction> entry : txIndex.entrySet()) {
                Transaction tx = entry.getValue();
                if ("register".equals(tx.getTxType())
                    && statuses.contains(tx.getStatus())
                    && (tx.getDeviceId() == null || !excluded.contains(tx.getDeviceId()))) {
                    anchors.add(entry.getKey());
                }
            }
            Collections.sort(anchors, new Comparator<String>() {
                @Override
                public int compare(String left, String right) {
                    int byDegree = Integer.compare(childrenByTx(left).size(), childrenByTx(right).size());
                    if (byDegree != 0) {
                        return byDegree;
                    }
                    Transaction leftTx = txIndex.get(left);
                    Transaction rightTx = txIndex.get(right);
                    int byTime = Double.compare(leftTx.getTimestamp(), rightTx.getTimestamp());
                    if (byTime != 0) {
                        return byTime;
                    }
                    return left.compareTo(right);
                }
            });
            return anchors;
        } finally {
            lock.unlock();
        }
    }

    private List<String> ensureParentCount(
        Collection<String> parentIds,
        int minimum,
        Set<String> allowedParentTypes,
        Set<String> excludedTxIds,
        Set<String> excludeDeviceIds,
        Set<String> includeRegisterStatuses,
        boolean allowRegisterFill
    ) {
        lock.lock();
        try {
            List<String> unique = new ArrayList<String>();
            Set<String> seen = new LinkedHashSet<String>();
            Set<String> blocked = excludedTxIds == null ? Collections.<String>emptySet() : excludedTxIds;
            for (String parentId : parentIds) {
                if (!txIndex.containsKey(parentId) || seen.contains(parentId) || blocked.contains(parentId)) {
                    continue;
                }
                if (allowedParentTypes != null && !allowedParentTypes.contains(txIndex.get(parentId).getTxType())) {
                    continue;
                }
                unique.add(parentId);
                seen.add(parentId);
            }
            if (unique.size() >= minimum) {
                return unique;
            }
            if (allowRegisterFill) {
                for (String anchorId : selectRegisterAnchors(
                    excludeDeviceIds,
                    includeRegisterStatuses == null ? setOf("pending", "confirmed") : includeRegisterStatuses
                )) {
                    if (seen.contains(anchorId) || blocked.contains(anchorId)) {
                        continue;
                    }
                    if (allowedParentTypes != null && !allowedParentTypes.contains(txIndex.get(anchorId).getTxType())) {
                        continue;
                    }
                    unique.add(anchorId);
                    seen.add(anchorId);
                    if (unique.size() >= minimum) {
                        return unique;
                    }
                }
            }
            if (genesisBootstrapOpen()) {
                for (String genesisId : GLOBAL_GENESIS_IDS) {
                    if (txIndex.containsKey(genesisId) && !seen.contains(genesisId) && !blocked.contains(genesisId)) {
                        if (allowedParentTypes != null && !allowedParentTypes.contains(txIndex.get(genesisId).getTxType())) {
                            continue;
                        }
                        unique.add(genesisId);
                        seen.add(genesisId);
                    }
                    if (unique.size() >= minimum) {
                        return unique;
                    }
                }
            }
            for (String txId : orderedTxIds(txIndex.keySet())) {
                if (seen.contains(txId) || blocked.contains(txId)) {
                    continue;
                }
                if (allowedParentTypes != null && !allowedParentTypes.contains(txIndex.get(txId).getTxType())) {
                    continue;
                }
                if ("genesis".equals(txIndex.get(txId).getTxType()) && !genesisBootstrapOpen()) {
                    continue;
                }
                unique.add(txId);
                seen.add(txId);
                if (unique.size() >= minimum) {
                    break;
                }
            }
            return unique;
        } finally {
            lock.unlock();
        }
    }

    private boolean validateParentStructure(Transaction tx) {
        if ("register".equals(tx.getTxType())) {
            if (new LinkedHashSet<String>(tx.getParentIds()).size() != 2) {
                return false;
            }
            for (String parentId : tx.getParentIds()) {
                Transaction parent = txIndex.get(parentId);
                if (parent == null || (!"genesis".equals(parent.getTxType()) && !"register".equals(parent.getTxType()))) {
                    return false;
                }
            }
            if (!genesisBootstrapOpen()) {
                for (String parentId : tx.getParentIds()) {
                    if ("genesis".equals(txIndex.get(parentId).getTxType())) {
                        return false;
                    }
                }
            }
            return true;
        }
        if (!"business".equals(tx.getTxType())) {
            return true;
        }
        Map<String, Object> businessPayload = tx.getPayload() == null ? null : mapValue(tx.getPayload().get("business_payload"));
        String identityRef = tx.getPayload() == null ? null : stringValue(tx.getPayload().get("identity_ref_tx_id"));
        String authRef = businessPayload == null ? null : stringValue(businessPayload.get("auth_ref_tx_id"));
        String anchorId = authRef != null ? authRef : identityRef;
        List<String> conventionalParents = new ArrayList<String>();
        for (String parentId : tx.getParentIds()) {
            if (!Objects.equals(parentId, anchorId)) {
                conventionalParents.add(parentId);
            }
        }
        if (new LinkedHashSet<String>(tx.getParentIds()).size() != tx.getParentIds().size()) {
            return false;
        }
        if (tx.getParentIds().size() < 2 || tx.getParentIds().size() > 3) {
            return false;
        }
        if (conventionalParents.size() < 1 || anchorId == null || !tx.getParentIds().contains(anchorId)) {
            return false;
        }
        if (!genesisBootstrapOpen()) {
            for (String parentId : conventionalParents) {
                Transaction parent = txIndex.get(parentId);
                if (parent != null && "genesis".equals(parent.getTxType())) {
                    return false;
                }
            }
        }
        Transaction anchorTx = txIndex.get(anchorId);
        return anchorTx != null
            && "register".equals(anchorTx.getTxType())
            && "confirmed".equals(anchorTx.getStatus())
            && (tx.getDeviceId() == null || Objects.equals(anchorTx.getDeviceId(), tx.getDeviceId()));
    }

    private List<String> increaseAncestorWeights(String txId) {
        List<String> ancestors = orderedTxIds(ancestorsOf(txId));
        List<String> updatedIds = new ArrayList<String>();
        for (String ancestorId : ancestors) {
            Transaction tx = txIndex.get(ancestorId);
            if (tx != null && !"genesis".equals(tx.getTxType()) && !"hard_deleted".equals(tx.getStatus())) {
                tx.setCumulativeWeight(tx.getCumulativeWeight() + 1);
                weightIndex.put(ancestorId, Integer.valueOf(tx.getCumulativeWeight()));
                updatedIds.add(ancestorId);
            }
        }
        return updatedIds;
    }

    private boolean childLive(String txId) {
        for (String childId : childrenByTx(txId)) {
            Transaction child = txIndex.get(childId);
            if (child != null && ("pending".equals(child.getStatus()) || "confirmed".equals(child.getStatus()))) {
                return true;
            }
        }
        return false;
    }

    private int refCount(String txId) {
        int count = 0;
        for (String childId : childrenByTx(txId)) {
            Transaction child = txIndex.get(childId);
            if (child != null && ("pending".equals(child.getStatus()) || "confirmed".equals(child.getStatus()))) {
                count++;
            }
        }
        return count;
    }

    private boolean isCovered(String txId) {
        List<Transaction> successors = new ArrayList<Transaction>();
        for (String childId : childrenByTx(txId)) {
            Transaction child = txIndex.get(childId);
            if (child != null) {
                successors.add(child);
            }
        }
        if (successors.isEmpty()) {
            return false;
        }
        for (Transaction child : successors) {
            if (!"soft_deleted".equals(child.getStatus()) && !"hard_deleted".equals(child.getStatus())) {
                return false;
            }
        }
        return true;
    }

    private boolean canHardDelete(String txId) {
        Transaction tx = txIndex.get(txId);
        return tx != null
            && "business".equals(tx.getTxType())
            && "soft_deleted".equals(tx.getStatus())
            && !childLive(txId)
            && refCount(txId) == 0
            && isCovered(txId);
    }

    public Map<String, Transaction> getTxIndex() {
        return txIndex;
    }

    public Map<String, Integer> getWeightIndex() {
        return weightIndex;
    }

    public Map<String, String> getDeviceRegistry() {
        return deviceRegistry;
    }

    public Set<String> getTipSet() {
        return tipSet;
    }

    public ReentrantLock getLock() {
        return lock;
    }

    public String getTerminalId() {
        return terminalId;
    }

    public String getTerminalSignPrivkeyForTesting() {
        return terminalSignPrivkey;
    }

    private void initGenesis() {
        double base = now();
        for (int index = 0; index < GLOBAL_GENESIS_IDS.size(); index++) {
            String genesisId = GLOBAL_GENESIS_IDS.get(index);
            Transaction tx = new Transaction();
            tx.setTxId(genesisId);
            tx.setTxType("genesis");
            tx.setSourceTerminalId("GLOBAL");
            tx.setSourceTerminalPubkey("GENESIS");
            tx.setParentIds(new ArrayList<String>());
            tx.setTimestamp(base + index * 0.001);
            tx.setCreatedRound(0);
            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("genesis", genesisId);
            tx.setPayloadHash(CryptoUtils.sha256Hex(payload));
            tx.setTerminalSignature("GENESIS");
            tx.setPayload(payload);
            tx.setCumulativeWeight(0);
            tx.setStatus("confirmed");
            txIndex.put(tx.getTxId(), tx);
            weightIndex.put(tx.getTxId(), Integer.valueOf(tx.getCumulativeWeight()));
            parentsByTx.put(tx.getTxId(), new LinkedHashSet<String>());
            childrenByTx.put(tx.getTxId(), new LinkedHashSet<String>());
            tipSet.add(tx.getTxId());
        }
    }

    private Transaction buildSignedTx(
        String txType,
        Map<String, Object> payload,
        List<String> parentIds,
        String deviceId,
        String devicePubkey,
        String targetTerminalId,
        Map<String, Object> attributes
    ) {
        Transaction tx = new Transaction();
        tx.setTxId(CryptoUtils.sha256Hex(payload));
        tx.setTxType(txType);
        tx.setSourceTerminalId(terminalId);
        tx.setSourceTerminalPubkey(terminalSignPubkey);
        tx.setParentIds(new ArrayList<String>(parentIds));
        tx.setTimestamp(numberValue(payload.get("timestamp")));
        tx.setCreatedRound(((Number) payload.get("round")).intValue());
        tx.setPayloadHash(CryptoUtils.sha256Hex(payload));
        tx.setTerminalSignature(CryptoUtils.secpSign(terminalSignPrivkey, payload));
        tx.setDeviceId(deviceId);
        tx.setDevicePubkey(devicePubkey);
        tx.setTargetTerminalId(targetTerminalId);
        tx.setAttributes(attributes);
        tx.setPayload(payload);
        return tx;
    }

    private Set<String> parentsByTx(String txId) {
        return parentsByTx.containsKey(txId) ? parentsByTx.get(txId) : Collections.<String>emptySet();
    }

    private Set<String> childrenByTx(String txId) {
        return childrenByTx.containsKey(txId) ? childrenByTx.get(txId) : Collections.<String>emptySet();
    }

    private void linkParent(String parentId, String childId) {
        if (!parentsByTx.containsKey(childId)) {
            parentsByTx.put(childId, new LinkedHashSet<String>());
        }
        if (!childrenByTx.containsKey(parentId)) {
            childrenByTx.put(parentId, new LinkedHashSet<String>());
        }
        parentsByTx.get(childId).add(parentId);
        childrenByTx.get(parentId).add(childId);
        tipSet.remove(parentId);
        tipSet.add(childId);
    }

    private void unlinkParent(String parentId, String childId) {
        if (parentsByTx.containsKey(childId)) {
            parentsByTx.get(childId).remove(parentId);
        }
        if (childrenByTx.containsKey(parentId)) {
            childrenByTx.get(parentId).remove(childId);
            if (childrenByTx.get(parentId).isEmpty() && txIndex.containsKey(parentId)) {
                tipSet.add(parentId);
            }
        }
    }

    private Set<String> ancestorsOf(String txId) {
        Set<String> visited = new LinkedHashSet<String>();
        ArrayDeque<String> queue = new ArrayDeque<String>(parentsByTx(txId));
        while (!queue.isEmpty()) {
            String currentId = queue.removeFirst();
            if (!visited.add(currentId)) {
                continue;
            }
            queue.addAll(parentsByTx(currentId));
        }
        return visited;
    }

    private List<String> orderedTxIds(Collection<String> txIds) {
        List<String> ordered = new ArrayList<String>();
        for (String txId : txIds) {
            if (txIndex.containsKey(txId)) {
                ordered.add(txId);
            }
        }
        Collections.sort(ordered, new Comparator<String>() {
            @Override
            public int compare(String left, String right) {
                Transaction leftTx = txIndex.get(left);
                Transaction rightTx = txIndex.get(right);
                int byTime = Double.compare(leftTx.getTimestamp(), rightTx.getTimestamp());
                if (byTime != 0) {
                    return byTime;
                }
                return left.compareTo(right);
            }
        });
        return ordered;
    }

    private String pickWeighted(List<WeightedEdge> weightedEdges) {
        double total = 0.0;
        for (WeightedEdge edge : weightedEdges) {
            total += edge.weight;
        }
        double target = random.nextDouble() * total;
        double seen = 0.0;
        for (WeightedEdge edge : weightedEdges) {
            seen += edge.weight;
            if (target <= seen) {
                return edge.txId;
            }
        }
        return weightedEdges.get(weightedEdges.size() - 1).txId;
    }

    private static double now() {
        return System.currentTimeMillis() / 1000.0;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapCopy(Map<String, Object> source) {
        return source == null ? null : (Map<String, Object>) JsonCanonicalizer.deepCopy(source);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapValue(Object value) {
        return value instanceof Map<?, ?> ? (Map<String, Object>) value : null;
    }

    private static List<String> listValue(Object value) {
        if (!(value instanceof Collection<?>)) {
            return null;
        }
        List<String> result = new ArrayList<String>();
        for (Object item : (Collection<?>) value) {
            result.add(String.valueOf(item));
        }
        return result;
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static double numberValue(Object value) {
        return value instanceof Number ? ((Number) value).doubleValue() : Double.parseDouble(String.valueOf(value));
    }

    @SafeVarargs
    private static <T> Set<T> setOf(T... values) {
        return new LinkedHashSet<T>(Arrays.asList(values));
    }

    private static final class WeightedEdge {
        private final String txId;
        private final double weight;

        private WeightedEdge(String txId, double weight) {
            this.txId = txId;
            this.weight = weight;
        }
    }

    public static final class InsertResult {
        private final boolean inserted;
        private final List<String> candidates;

        private InsertResult(boolean inserted, List<String> candidates) {
            this.inserted = inserted;
            this.candidates = candidates;
        }

        public boolean isInserted() {
            return inserted;
        }

        public List<String> getCandidates() {
            return candidates;
        }
    }
}

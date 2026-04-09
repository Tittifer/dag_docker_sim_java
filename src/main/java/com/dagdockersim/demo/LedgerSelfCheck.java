package com.dagdockersim.demo;

import com.dagdockersim.crypto.CryptoUtils;
import com.dagdockersim.crypto.KeyPairStrings;
import com.dagdockersim.ledger.DagLedger;
import com.dagdockersim.model.LifecycleAction;
import com.dagdockersim.model.Transaction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class LedgerSelfCheck {
    private LedgerSelfCheck() {
    }

    public static void main(String[] args) {
        testBusinessTransactionBindsIdentityAnchor();
        testRejectsBusinessWithoutIdentityAnchorParent();
        testGenesisOnlyUsedForFirstTwoNonGenesisTransactions();
        testCloudLifecycleOnlyEmitsSoftDeleteNotice();
        testLocalPruningHardDeletesSoftDeletedTransactions();
        testLargeWeightsRemainFinite();
        System.out.println("Self-check passed.");
    }

    private static void testBusinessTransactionBindsIdentityAnchor() {
        DagLedger ledger = makeLedger(30, 5, false, 7L);
        String regTxId = confirmDeviceRegistration(ledger, "device-A");
        Transaction businessTx = ledger.createBusinessTx(payload("device-A", 1.0, 12.5), "device-A", null);
        assertEquals("device-A", businessTx.getDeviceId(), "device id binding");
        assertEquals(regTxId, businessTx.getPayload().get("identity_ref_tx_id"), "identity ref");
        assertEquals(regTxId, mapValue(businessTx.getPayload().get("business_payload")).get("auth_ref_tx_id"), "auth ref");
        assertTrue(businessTx.getParentIds().contains(regTxId), "identity anchor parent missing");
        assertTrue(businessTx.getParentIds().size() >= 2, "business parent size");
        assertTrue(ledger.insertTransaction(businessTx), "business insert");
    }

    private static void testRejectsBusinessWithoutIdentityAnchorParent() {
        DagLedger ledger = makeLedger(30, 5, false, 7L);
        confirmDeviceRegistration(ledger, "device-A");
        Transaction invalidTx = ledger.createBusinessTx(payload("device-A", 2.0, 9.1), "device-A", null);
        String identityRef = String.valueOf(invalidTx.getPayload().get("identity_ref_tx_id"));
        List<String> mutatedParents = new ArrayList<String>();
        for (String parentId : invalidTx.getParentIds()) {
            if (!identityRef.equals(parentId)) {
                mutatedParents.add(parentId);
            }
        }
        invalidTx.setParentIds(mutatedParents);
        invalidTx.getPayload().put("parent_ids", new ArrayList<String>(mutatedParents));
        resignTransaction(ledger, invalidTx);
        assertTrue(!ledger.insertTransaction(invalidTx), "invalid business should be rejected");
    }

    private static void testGenesisOnlyUsedForFirstTwoNonGenesisTransactions() {
        DagLedger ledger = makeLedger(30, 5, false, 5L);
        KeyPairStrings firstKeys = CryptoUtils.generateSecp256k1KeyPair();
        Transaction firstReg = ledger.createRegisterTx("device-A", firstKeys.getPublicKey(), role());
        assertEquals(set("GENESIS_GLOBAL_A", "GENESIS_GLOBAL_B"), new LinkedHashSet<String>(firstReg.getParentIds()), "first register parents");
        assertTrue(ledger.insertTransaction(firstReg), "first register insert");
        assertTrue(ledger.confirmRegister(firstReg.getTxId()), "first register confirm");

        KeyPairStrings secondKeys = CryptoUtils.generateSecp256k1KeyPair();
        Transaction secondReg = ledger.createRegisterTx("device-B", secondKeys.getPublicKey(), role());
        assertTrue(secondReg.getParentIds().contains("GENESIS_GLOBAL_A") || secondReg.getParentIds().contains("GENESIS_GLOBAL_B"), "second register genesis reference");
        assertTrue(ledger.insertTransaction(secondReg), "second register insert");
        assertTrue(ledger.confirmRegister(secondReg.getTxId()), "second register confirm");

        KeyPairStrings thirdKeys = CryptoUtils.generateSecp256k1KeyPair();
        Transaction thirdReg = ledger.createRegisterTx("device-C", thirdKeys.getPublicKey(), role());
        for (String parentId : thirdReg.getParentIds()) {
            assertTrue(!"GENESIS_GLOBAL_A".equals(parentId) && !"GENESIS_GLOBAL_B".equals(parentId), "third register should not use genesis");
        }

        Transaction businessTx = ledger.createBusinessTx(payload("device-A", 4.0, 7.4), "device-A", null);
        assertTrue(ledger.insertTransaction(businessTx), "business insert");
        for (int index = 0; index < Math.min(2, businessTx.getParentIds().size()); index++) {
            String parentId = businessTx.getParentIds().get(index);
            assertTrue(!"GENESIS_GLOBAL_A".equals(parentId) && !"GENESIS_GLOBAL_B".equals(parentId), "business conventional parents should not use genesis");
        }
        assertEquals(Integer.valueOf(0), ledger.transactionWeight("GENESIS_GLOBAL_A"), "genesis weight A");
        assertEquals(Integer.valueOf(0), ledger.transactionWeight("GENESIS_GLOBAL_B"), "genesis weight B");
    }

    private static void testCloudLifecycleOnlyEmitsSoftDeleteNotice() {
        DagLedger ledger = makeLedger(99, 2, true, 7L);
        String regTxId = confirmDeviceRegistration(ledger, "device-A");
        String regTxIdB = confirmDeviceRegistration(ledger, "device-B");

        Transaction firstTx = ledger.createBusinessTx(payload("device-A", 10.0, 1.0), "device-A", null);
        assertTrue(ledger.insertTransaction(firstTx), "first business insert");

        Transaction secondTx = ledger.createBusinessTx(payload("device-A", 11.0, 2.0), "device-A", null);
        secondTx.setParentIds(list(firstTx.getTxId(), regTxIdB, regTxId));
        secondTx.getPayload().put("parent_ids", list(firstTx.getTxId(), regTxIdB, regTxId));
        resignTransaction(ledger, secondTx);
        assertTrue(ledger.insertTransaction(secondTx), "second business insert");

        List<LifecycleAction> actions = ledger.applyCloudLifecycle();
        assertContains(actions, "confirm_business", firstTx.getTxId());
        assertContains(actions, "soft_delete_business", firstTx.getTxId());
        assertEquals("confirmed", ledger.getTxIndex().get(firstTx.getTxId()).getStatus(), "first tx status after cloud lifecycle");
        assertTrue(ledger.getTxIndex().get(firstTx.getTxId()).getSoftPruneNotifiedAt() != null, "soft prune notice timestamp");
        assertTrue(ledger.getTxIndex().get(firstTx.getTxId()).getSoftPrunedAt() == null, "soft delete should not be applied by cloud");
    }

    private static void testLocalPruningHardDeletesSoftDeletedTransactions() {
        DagLedger ledger = makeLedger(99, 2, true, 7L);
        String regTxId = confirmDeviceRegistration(ledger, "device-A");
        String regTxIdB = confirmDeviceRegistration(ledger, "device-B");

        Transaction firstTx = ledger.createBusinessTx(payload("device-A", 20.0, 1.0), "device-A", null);
        assertTrue(ledger.insertTransaction(firstTx), "first business insert");

        Transaction secondTx = ledger.createBusinessTx(payload("device-A", 21.0, 2.0), "device-A", null);
        secondTx.setParentIds(list(firstTx.getTxId(), regTxIdB, regTxId));
        secondTx.getPayload().put("parent_ids", list(firstTx.getTxId(), regTxIdB, regTxId));
        resignTransaction(ledger, secondTx);
        assertTrue(ledger.insertTransaction(secondTx), "second business insert");

        Transaction thirdTx = ledger.createBusinessTx(payload("device-A", 22.0, 3.0), "device-A", null);
        thirdTx.setParentIds(list(secondTx.getTxId(), regTxIdB, regTxId));
        thirdTx.getPayload().put("parent_ids", list(secondTx.getTxId(), regTxIdB, regTxId));
        resignTransaction(ledger, thirdTx);
        assertTrue(ledger.insertTransaction(thirdTx), "third business insert");

        assertTrue(ledger.confirmBusiness(firstTx.getTxId()), "confirm first");
        assertTrue(ledger.softDeleteBusiness(firstTx.getTxId()), "soft delete first");
        assertTrue(ledger.confirmBusiness(secondTx.getTxId()), "confirm second");
        assertTrue(ledger.softDeleteBusiness(secondTx.getTxId()), "soft delete second");
        assertTrue(ledger.confirmBusiness(thirdTx.getTxId()), "confirm third");
        assertTrue(ledger.softDeleteBusiness(thirdTx.getTxId()), "soft delete third");

        List<LifecycleAction> actions = ledger.applyLocalPruning();
        assertContains(actions, "hard_delete_business", firstTx.getTxId());
        Transaction retained = ledger.getTxIndex().get(firstTx.getTxId());
        assertEquals("hard_deleted", retained.getStatus(), "first tx should become hard deleted");
        assertTrue(retained.getHardPrunedAt() != null, "hard prune timestamp");
    }

    private static void testLargeWeightsRemainFinite() {
        DagLedger ledger = makeLedger(30, 5, false, 7L);
        String regTxId = confirmDeviceRegistration(ledger, "device-A");
        confirmDeviceRegistration(ledger, "device-B");

        Transaction businessTx = ledger.createBusinessTx(payload("device-A", 13.0, 12.5), "device-A", null);
        assertTrue(ledger.insertTransaction(businessTx), "business insert");

        for (Transaction tx : ledger.getTxIndex().values()) {
            if (!"genesis".equals(tx.getTxType())) {
                tx.setCumulativeWeight(10_000);
            }
        }
        List<String> parents = ledger.selectBusinessParents("device-A", regTxId);
        assertEquals(Integer.valueOf(3), Integer.valueOf(parents.size()), "business parents size");
        assertTrue(parents.contains(regTxId), "business parents should keep anchor");
    }

    private static DagLedger makeLedger(int regThreshold, int dataThreshold, boolean preserveFull, long randomSeed) {
        KeyPairStrings signer = CryptoUtils.generateSecp256k1KeyPair();
        return new DagLedger(
            "fusion-test",
            signer.getPrivateKey(),
            signer.getPublicKey(),
            regThreshold,
            dataThreshold,
            false,
            preserveFull,
            0.4,
            1.25,
            Long.valueOf(randomSeed)
        );
    }

    private static String confirmDeviceRegistration(DagLedger ledger, String deviceId) {
        KeyPairStrings deviceKeys = CryptoUtils.generateSecp256k1KeyPair();
        Transaction regTx = ledger.createRegisterTx(deviceId, deviceKeys.getPublicKey(), role());
        assertTrue(ledger.insertTransaction(regTx), "register insert");
        assertTrue(ledger.confirmRegister(regTx.getTxId()), "register confirm");
        return regTx.getTxId();
    }

    private static void resignTransaction(DagLedger ledger, Transaction tx) {
        tx.setPayloadHash(CryptoUtils.sha256Hex(tx.getPayload()));
        tx.setTxId(CryptoUtils.sha256Hex(tx.getPayload()));
        tx.setTerminalSignature(CryptoUtils.secpSign(ledger.getTerminalSignPrivkeyForTesting(), tx.getPayload()));
    }

    private static Map<String, Object> payload(String deviceId, double requestTs, double powerKw) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("device_id", deviceId);
        payload.put("request_ts", Double.valueOf(requestTs));
        Map<String, Object> dataPayload = new LinkedHashMap<String, Object>();
        dataPayload.put("power_kw", Double.valueOf(powerKw));
        payload.put("data_payload", dataPayload);
        return payload;
    }

    private static Map<String, Object> role() {
        Map<String, Object> role = new LinkedHashMap<String, Object>();
        role.put("role", "edge_device");
        return role;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapValue(Object value) {
        return (Map<String, Object>) value;
    }

    private static List<String> list(String... values) {
        List<String> list = new ArrayList<String>();
        for (String value : values) {
            list.add(value);
        }
        return list;
    }

    private static Set<String> set(String... values) {
        Set<String> set = new LinkedHashSet<String>();
        for (String value : values) {
            set.add(value);
        }
        return set;
    }

    private static void assertContains(List<LifecycleAction> actions, String actionName, String txId) {
        for (LifecycleAction action : actions) {
            if (actionName.equals(action.getAction()) && txId.equals(action.getTxId())) {
                return;
            }
        }
        throw new IllegalStateException("expected action not found: " + actionName + " for " + txId);
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new IllegalStateException(message + " expected=" + expected + " actual=" + actual);
        }
    }
}

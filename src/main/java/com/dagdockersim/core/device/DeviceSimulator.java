package com.dagdockersim.core.device;

import com.dagdockersim.core.bootstrap.BootstrapEnvironment;
import com.dagdockersim.core.crypto.CryptoUtils;
import com.dagdockersim.core.crypto.KeyPairStrings;
import com.dagdockersim.core.fusion.FusionTerminal;
import com.dagdockersim.model.domain.Transaction;

import java.util.LinkedHashMap;
import java.util.Map;

public class DeviceSimulator {
    private final String deviceName;
    private final String deviceId;
    private final String signPrivkey;
    private final String signPubkey;

    private DeviceSimulator(String deviceName, String deviceId, String signPrivkey, String signPubkey) {
        this.deviceName = deviceName;
        this.deviceId = deviceId;
        this.signPrivkey = signPrivkey;
        this.signPubkey = signPubkey;
    }

    public static DeviceSimulator newDynamic(String deviceName) {
        KeyPairStrings keys = CryptoUtils.generateSecp256k1KeyPair();
        return new DeviceSimulator(deviceName, CryptoUtils.randomId(deviceName), keys.getPrivateKey(), keys.getPublicKey());
    }

    public static DeviceSimulator newBootstrap(String deviceName) {
        KeyPairStrings keys = BootstrapEnvironment.bootstrapDeviceKeyPair(deviceName);
        return new DeviceSimulator(deviceName, BootstrapEnvironment.bootstrapDeviceId(deviceName), keys.getPrivateKey(), keys.getPublicKey());
    }

    public static DeviceSimulator restore(String deviceName, String deviceId, String signPrivkey, String signPubkey) {
        return new DeviceSimulator(deviceName, deviceId, signPrivkey, signPubkey);
    }

    public Transaction registerAt(FusionTerminal terminal) {
        return terminal.registerDevice(deviceId, signPubkey);
    }

    public Transaction submitTelemetry(FusionTerminal terminal, Map<String, Object> dataPayload) {
        double requestTs = System.currentTimeMillis() / 1000.0;
        Map<String, Object> authBody = new LinkedHashMap<String, Object>();
        authBody.put("device_id", deviceId);
        authBody.put("data_payload", dataPayload);
        authBody.put("request_ts", Double.valueOf(requestTs));
        String signature = CryptoUtils.secpSign(signPrivkey, authBody);
        return terminal.submitDeviceData(deviceId, dataPayload, signature, requestTs);
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getSignPubkey() {
        return signPubkey;
    }

    public String getSignPrivkey() {
        return signPrivkey;
    }
}



package com.dagdockersim.core.crypto;

public class KeyPairStrings {
    private final String privateKey;
    private final String publicKey;

    public KeyPairStrings(String privateKey, String publicKey) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public String getPublicKey() {
        return publicKey;
    }
}


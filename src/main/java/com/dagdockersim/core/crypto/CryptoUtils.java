package com.dagdockersim.core.crypto;

import com.dagdockersim.utils.JsonCanonicalizer;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Random;

public final class CryptoUtils {
    private static final Random RANDOM = new Random();

    private CryptoUtils() {
    }

    public static String canonicalJson(Object data) {
        return JsonCanonicalizer.canonicalJson(data);
    }

    public static String sha256Hex(Object data) {
        byte[] payload;
        if (data instanceof byte[]) {
            payload = (byte[]) data;
        } else if (data instanceof String) {
            payload = ((String) data).getBytes(StandardCharsets.UTF_8);
        } else {
            payload = canonicalJson(data).getBytes(StandardCharsets.UTF_8);
        }
        return bytesToHex(sha256(payload));
    }

    public static String randomId(String prefix) {
        String seed = prefix + "-" + System.nanoTime() + "-" + RANDOM.nextDouble();
        return sha256Hex(seed).substring(0, 20);
    }

    public static KeyPairStrings generateSecp256k1KeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec("secp256k1"));
            return serialize(generator.generateKeyPair());
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("failed_to_generate_secp256k1_keypair", exception);
        }
    }

    public static KeyPairStrings deriveSecp256k1KeyPair(String seed) {
        try {
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            random.setSeed(seed.getBytes(StandardCharsets.UTF_8));
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec("secp256k1"), random);
            return serialize(generator.generateKeyPair());
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("failed_to_derive_secp256k1_keypair", exception);
        }
    }

    public static String secpSign(String privateKeyBase64, Object data) {
        try {
            Signature signature = Signature.getInstance("SHA256withECDSA");
            signature.initSign(loadPrivateKey(privateKeyBase64));
            signature.update(payloadBytes(data));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("failed_to_sign_payload", exception);
        }
    }

    public static boolean secpVerify(String publicKeyBase64, Object data, String signatureBase64) {
        try {
            Signature signature = Signature.getInstance("SHA256withECDSA");
            signature.initVerify(loadPublicKey(publicKeyBase64));
            signature.update(payloadBytes(data));
            return signature.verify(Base64.getDecoder().decode(signatureBase64));
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            return false;
        }
    }

    private static KeyPairStrings serialize(KeyPair keyPair) {
        String privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
        String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        return new KeyPairStrings(privateKey, publicKey);
    }

    private static PrivateKey loadPrivateKey(String privateKeyBase64) throws GeneralSecurityException {
        byte[] encoded = Base64.getDecoder().decode(privateKeyBase64);
        return KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(encoded));
    }

    private static PublicKey loadPublicKey(String publicKeyBase64) throws GeneralSecurityException {
        byte[] encoded = Base64.getDecoder().decode(publicKeyBase64);
        return KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(encoded));
    }

    private static byte[] payloadBytes(Object data) {
        if (data instanceof byte[]) {
            return (byte[]) data;
        }
        if (data instanceof String) {
            return ((String) data).getBytes(StandardCharsets.UTF_8);
        }
        return canonicalJson(data).getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("missing_sha256_digest", exception);
        }
    }

    private static String bytesToHex(byte[] data) {
        StringBuilder builder = new StringBuilder(data.length * 2);
        for (byte value : data) {
            builder.append(Character.forDigit((value >> 4) & 0xF, 16));
            builder.append(Character.forDigit(value & 0xF, 16));
        }
        return builder.toString();
    }
}


package co.nyzo.verifier.util;

import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSASecurityProvider;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.util.HashMap;
import java.util.Map;

public class SignatureUtil {

    private static final Map<ByteBuffer, EdDSAEngine> seedToSignatureMap = new HashMap<>();
    private static final Map<ByteBuffer, EdDSAEngine> identifierToSignatureMap = new HashMap<>();

    public static final EdDSAParameterSpec spec;

    static {
        Security.addProvider(new EdDSASecurityProvider());
        spec = EdDSANamedCurveTable.getByName("Ed25519");
    }

    public static byte[] signBytes(byte[] bytesToSign, byte[] privateSeed) {

        byte[] signatureBytes = null;

        try {
            ByteBuffer seedBuffer = ByteBuffer.wrap(privateSeed);
            EdDSAEngine signature = seedToSignatureMap.get(seedBuffer);
            if (signature == null) {
                signature = new EdDSAEngine(MessageDigest.getInstance(spec.getHashAlgorithm()));
                PrivateKey privateKey = KeyUtil.privateKeyFromSeed(privateSeed);
                signature.initSign(privateKey);
                seedToSignatureMap.put(seedBuffer, signature);
            }

            synchronized (SignatureUtil.class) {
                signatureBytes = signature.signOneShot(bytesToSign);
            }

        } catch (Exception reportOnly) {
            System.err.println("exception signing bytes of length " + (bytesToSign == null ? "(null)" :
                    bytesToSign.length) + " with seed " + ByteUtil.arrayAsStringWithDashes(privateSeed));
        }

        return signatureBytes;
    }

    public static boolean signatureIsValid(byte[] signatureBytes, byte[] signedBytes, byte[] publicIdentifier) {
        return signatureIsValid(signatureBytes, signedBytes, publicIdentifier, 0, signedBytes.length);
    }

    public static boolean signatureIsValid(byte[] signatureBytes, byte[] signedBytes, byte[] publicIdentifier,
                                           int signedBytesStart, int signedBytesEnd) {

        boolean signatureIsValid;

        try {
            ByteBuffer identifierBuffer = ByteBuffer.wrap(publicIdentifier);
            EdDSAEngine signature = identifierToSignatureMap.get(identifierBuffer);
            if (signature == null) {
                signature = new EdDSAEngine(MessageDigest.getInstance(spec.getHashAlgorithm()));
                PublicKey publicKey = KeyUtil.publicKeyFromIdentifier(publicIdentifier);
                signature.initVerify(publicKey);
                identifierToSignatureMap.put(identifierBuffer, signature);

                // If the map has gotten too big, remove an element from it.
                if (identifierToSignatureMap.size() > 20000) {
                    ByteBuffer key = identifierToSignatureMap.keySet().iterator().next();
                    identifierToSignatureMap.remove(key);
                }
            }

            synchronized (SignatureUtil.class) {
                int signedBytesLength = signedBytesEnd - signedBytesStart;
                signatureIsValid = signature.verifyOneShot(signedBytes, signedBytesStart, signedBytesLength,
                        signatureBytes, 0, signatureBytes.length);
            }

        } catch (Exception ignored) {

            signatureIsValid = false;
        }

        return signatureIsValid;
    }
}

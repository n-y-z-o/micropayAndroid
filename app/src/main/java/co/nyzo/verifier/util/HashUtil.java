package co.nyzo.verifier.util;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

public class HashUtil {

    private static MessageDigest messageDigest;
    static {
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
    }

    public static synchronized byte[] singleSHA256(byte[] data) {

        if (data == null) {
            data = new byte[0];
        }
        return messageDigest.digest(data);
    }

    public static synchronized byte[] doubleSHA256(byte[] data) {

        return messageDigest.digest(messageDigest.digest(data));
    }

    public static long longSHA256(byte[] data) {

        byte[] sha256 = singleSHA256(data);
        ByteBuffer buffer = ByteBuffer.wrap(sha256);
        return buffer.getLong();
    }

    public static long longSHA256(byte[]... dataArgs) {

        int length = 0;
        for (byte[] data : dataArgs) {
            length += data.length;
        }

        byte[] array = new byte[length];
        ByteBuffer buffer = ByteBuffer.wrap(array);
        for (byte[] data : dataArgs) {
            buffer.put(data);
        }

        return longSHA256(array);
    }

    public static byte[] bLongSHA256(byte[] data) {

        byte[] sha256 = singleSHA256(data);
        return Arrays.copyOf(sha256, 8);
    }

    public static byte[] bLongSHA256(byte[]... dataArgs) {

        int length = 0;
        for (byte[] data : dataArgs) {
            length += data.length;
        }

        byte[] array = new byte[length];
        ByteBuffer buffer = ByteBuffer.wrap(array);
        for (byte[] data : dataArgs) {
            buffer.put(data);
        }

        return bLongSHA256(array);
    }

    public static byte[] byteArray(int value) {

        byte[] array = new byte[4];
        ByteBuffer buffer = ByteBuffer.wrap(array);
        buffer.putInt(value);

        return array;
    }

    public static byte[] byteArray(long value) {

        byte[] array = new byte[8];
        ByteBuffer buffer = ByteBuffer.wrap(array);
        buffer.putLong(value);

        return array;
    }
}


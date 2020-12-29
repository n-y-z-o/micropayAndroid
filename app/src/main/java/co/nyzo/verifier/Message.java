package co.nyzo.verifier;

import java.nio.ByteBuffer;

public class Message {

    public static byte[] getByteArray(ByteBuffer buffer, int size) {

        byte[] array = new byte[size];
        buffer.get(array);

        return array;
    }
}

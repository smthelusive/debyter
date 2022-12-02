package smthelusive.debyter;

import java.nio.ByteBuffer;

public class Utils {
    public static byte[] getBytesOfInt(int value) {
        return ByteBuffer.allocate(4).putInt(value).array();
    }

    public static byte[] getBytesOfLong(long value) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(value);
        return buffer.array();
    }

    public static byte getByteOfInt(int value) {
        return (byte)((value) & 0xff);
    }

    public static int byteArrayToInteger(byte[] input) {
        return ByteBuffer.wrap(input).getInt();
    }

    public static long byteArrayToLong(byte[] input) {
        return ByteBuffer.wrap(input).getLong();
    }

    public static short byteArrayToShort(byte[] input) {
        return ByteBuffer.wrap(input).getShort();
    }
}

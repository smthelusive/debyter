package smthelusive.debyter;

import java.nio.ByteBuffer;

public class Utils {
    public static byte[] getBytesOfInt(int value) {
        byte[] b = new byte[4];
        b[0] = (byte)((value >>> 24) & 0xff);
        b[1] = (byte)((value >>> 16) & 0xff);
        b[2] = (byte)((value >>>  8) & 0xff);
        b[3] = (byte)((value) & 0xff);
        return b;
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

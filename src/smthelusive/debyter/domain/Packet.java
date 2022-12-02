package smthelusive.debyter.domain;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static smthelusive.debyter.Utils.*;

public class Packet {
    private final LinkedList<Byte> bytes = new LinkedList<>();

    public Packet(int id, int flags, int commandSet, int command) {
        setLength(11);
        setID(id);
        setFlagCmdSetCmd(flags, commandSet, command);
    }

    private void setLength(int length) {
        byte[] lengthBytes = getBytesOfInt(length);
        for (int i = 0; i < 4; i++) {
            if (bytes.size() >= 4)
                bytes.set(i, lengthBytes[i]);
            else bytes.add(i, lengthBytes[i]);
        }
    }

    private void setID(int id) {
        byte[] idBytes = getBytesOfInt(id);
        for (int i = 4; i < 8; i++) {
            bytes.add(i, idBytes[i - 4]);
        }
    }

    private void setFlagCmdSetCmd(int flags, int cmdset, int command) {
        bytes.add(8, getByteOfInt(flags));
        bytes.add(9, getByteOfInt(cmdset));
        bytes.add(10, getByteOfInt(command));
    }

    public void addDataAsInt(int value) {
        byte[] valueInBytes = getBytesOfInt(value);
        for (Byte theByte: valueInBytes) {
            bytes.add(bytes.size(), theByte);
        }
    }

    public void addDataAsLong(long value) {
        byte[] valueInBytes = getBytesOfLong(value);
        for (Byte theByte: valueInBytes) {
            bytes.add(bytes.size(), theByte);
        }
    }

    public void addDataAsByte(int value) {
        bytes.add(bytes.size(), getByteOfInt(value));
    }

    public void addDataAsBytes(byte[] toBeAdded) {
        for (Byte b: toBeAdded) bytes.add(b);
    }

    public void addDataAsBytes(String value) {
        byte[] valueBytes = value.getBytes();
        for (Byte b: valueBytes) bytes.add(b);
    }

    public byte[] getPacketBytes() {
        // update size since the data might have changed:
        setLength(bytes.size());
        byte[] result = new byte[bytes.size()];
        for (int i = 0; i < bytes.size(); i++) {
            result[i] = bytes.get(i);
        }
        return result;
    }
}

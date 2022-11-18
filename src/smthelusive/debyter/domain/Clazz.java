package smthelusive.debyter.domain;

import smthelusive.debyter.Utils;

public class Clazz {
    byte refTypeTag;
    long typeID;
    int status;

    String signature;

    public Clazz(byte refTypeTag, long typeID, String signature, int status) {
        this.refTypeTag = refTypeTag;
        this.typeID = typeID;
        this.signature = signature;
        this.status = status;
    }

    public byte getRefTypeTag() {
        return refTypeTag;
    }

    public void setRefTypeTag(byte refTypeTag) {
        this.refTypeTag = refTypeTag;
    }

    public long getTypeID() {
        return typeID;
    }

    public void setTypeID(long typeID) {
        this.typeID = typeID;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    @Override
    public String toString() {
        return "Clazz{" +
                "refTypeTag=" + refTypeTag +
                ", typeID=" + typeID +
                ", status=" + status +
                ", signature='" + signature + '\'' +
                '}';
    }
}

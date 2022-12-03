package smthelusive.debyter.domain;

public record AClass(byte refTypeTag, long typeID, String signature, int status) {
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
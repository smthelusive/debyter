package smthelusive.debyter.domain;

public record AMethod(long methodId, String name, String signature, int modBits) {
    @Override
    public String toString() {
        return "AMethod{" +
                "methodId=" + methodId +
                ", name='" + name + '\'' +
                ", signature='" + signature + '\'' +
                ", modBits=" + modBits +
                '}';
    }
}
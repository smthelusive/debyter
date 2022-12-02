package smthelusive.debyter.domain;

public class AMethod {
    private long methodId;
    private String name;
    private String signature;
    private int modBits;

    public AMethod(long methodId, String name, String signature, int modBits) {
        this.methodId = methodId;
        this.name = name;
        this.signature = signature;
        this.modBits = modBits;
    }

    public long getMethodId() {
        return methodId;
    }

    public void setMethodId(long methodId) {
        this.methodId = methodId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public int getModBits() {
        return modBits;
    }

    public void setModBits(int modBits) {
        this.modBits = modBits;
    }

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

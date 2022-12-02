package smthelusive.debyter.domain;

public class Location {
    private byte tag;
    private long classId;
    private long methodId;
    private long codeIndex;

    public Location(byte tag, long classId, long methodId, long codeIndex) {
        this.tag = tag;
        this.classId = classId;
        this.methodId = methodId;
        this.codeIndex = codeIndex;
    }

    public byte getTag() {
        return tag;
    }

    public void setTag(byte tag) {
        this.tag = tag;
    }

    public long getClassId() {
        return classId;
    }

    public void setClassId(long classId) {
        this.classId = classId;
    }

    public long getMethodId() {
        return methodId;
    }

    public void setMethodId(long methodId) {
        this.methodId = methodId;
    }

    public long getCodeIndex() {
        return codeIndex;
    }

    public void setCodeIndex(long codeIndex) {
        this.codeIndex = codeIndex;
    }

    @Override
    public String toString() {
        return "Location{" +
                "tag=" + tag +
                ", classId=" + classId +
                ", methodId=" + methodId +
                ", codeIndex=" + codeIndex +
                '}';
    }
}

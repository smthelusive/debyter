package smthelusive.debyter.domain;

public record Location(byte tag, long classId, long methodId, long codeIndex) {

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

package smthelusive.debyter.domain;

public record Variable(long codeIndex, String name, String signature, int length, int slot) {
    @Override
    public String toString() {
        return "Variable{" +
                "codeIndex=" + codeIndex +
                ", name='" + name + '\'' +
                ", signature='" + signature + '\'' +
                ", length=" + length +
                ", slot=" + slot +
                '}';
    }
}

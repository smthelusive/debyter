package smthelusive.debyter.domain;

public class StringVariable implements GenericVariable {
    private String value;

    public StringVariable(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "StringVariable{" +
                "value='" + value + '\'' +
                '}';
    }
}

package smthelusive.debyter.domain;

import java.util.Arrays;

public class ArrayVariable implements GenericVariable {
    private GenericVariable[] values;

    public ArrayVariable(GenericVariable[] values) {
        this.values = values;
    }

    @Override
    public String toString() {
        return "ArrayVariable{" +
                "values=" + Arrays.toString(values) +
                '}';
    }
}

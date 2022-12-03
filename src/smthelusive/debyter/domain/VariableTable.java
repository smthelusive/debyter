package smthelusive.debyter.domain;

import java.util.ArrayList;
import java.util.List;

public class VariableTable {
    private int argCnt; // slots used by arguments
    private int slots;
    private List<Variable> variables;

    public int getArgCnt() {
        return argCnt;
    }

    public void setArgCnt(int argCnt) {
        this.argCnt = argCnt;
    }

    public int getSlots() {
        return slots;
    }

    public void setSlots(int slots) {
        this.slots = slots;
    }

    public List<Variable> getVariables() {
        return variables;
    }

    public void addVariable(Variable variable) {
        if (variables == null) variables = new ArrayList<>();
        variables.add(variable);
    }

    public void addVariable(long codeIndex, String name, String signature, int length, int slot) {
        if (variables == null) variables = new ArrayList<>();
        Variable variable = new Variable(codeIndex, name, signature, length, slot);
        variables.add(variable);
    }

    @Override
    public String toString() {
        return "VariableTable{" +
                "argCnt=" + argCnt +
                ", slots=" + slots +
                ", variables=" + variables +
                '}';
    }
}

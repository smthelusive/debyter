package smthelusive.debyter.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CurrentState {
    private long classId;
    private Location location;
    private byte[] bytecodes;
    private VariableTable variableTable;
    private long threadId;
    private List<AMethod> methods = new ArrayList<>();

    public long getClassId() {
        return classId;
    }

    public void setClassId(long classId) {
        this.classId = classId;
    }

    public List<AMethod> getMethods() {
        return methods;
    }

    public void setMethods(List<AMethod> methods) {
        this.methods = methods;
    }

    public Optional<Location> getLocation() {
        return Optional.ofNullable(location);
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Optional<byte[]> getBytecodes() {
        return Optional.ofNullable(bytecodes);
    }

    public void setBytecodes(byte[] bytecodes) {
        this.bytecodes = bytecodes;
    }

    public Optional<VariableTable> getVariableTable() {
        return Optional.ofNullable(variableTable);
    }

    public void setVariableTable(VariableTable variableTable) {
        this.variableTable = variableTable;
    }

    public long getThreadId() {
        return threadId;
    }

    public void setThreadId(long threadId) {
        this.threadId = threadId;
    }
}

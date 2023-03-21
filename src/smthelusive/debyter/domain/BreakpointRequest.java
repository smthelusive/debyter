package smthelusive.debyter.domain;

import java.util.Objects;

public record BreakpointRequest(int id, long classId, long methodId, long codeIndex) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BreakpointRequest that = (BreakpointRequest) o;
        return classId == that.classId && methodId == that.methodId && codeIndex == that.codeIndex && id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(classId, methodId, codeIndex, id);
    }
}

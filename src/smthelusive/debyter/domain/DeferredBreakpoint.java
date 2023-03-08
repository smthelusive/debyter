package smthelusive.debyter.domain;

import java.util.Objects;

public record DeferredBreakpoint(String className, String methodName, long codeIndex) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeferredBreakpoint that = (DeferredBreakpoint) o;
        return codeIndex == that.codeIndex && className.equals(that.className) && methodName.equals(that.methodName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, methodName, codeIndex);
    }
}

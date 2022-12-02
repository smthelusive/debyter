package smthelusive.debyter.domain;

import java.util.HashMap;
import java.util.Map;

public class LineTable {
    private long start;
    private long end;

    private Map<Long, Integer> lines = new HashMap<>();

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public Map<Long, Integer> getLines() {
        return lines;
    }

    public void addLine(Long lineCodeIndex, Integer lineNumber) {
        lines.put(lineCodeIndex, lineNumber);
    }

    @Override
    public String toString() {
        return "LineTable{" +
                "start=" + start +
                ", end=" + end +
                ", lines=" + lines +
                '}';
    }
}

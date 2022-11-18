package smthelusive.debyter.domain;

public class EventKind {
    public static final byte SINGLE_STEP = 1;
    public static final byte BREAKPOINT = 2;
    public static final byte FRAME_POP = 3;
    public static final byte EXCEPTION = 4;
    public static final byte USER_DEFINED = 5;
    public static final byte THREAD_START = 6;
    public static final byte THREAD_DEATH = 7;
    public static final byte CLASS_PREPARE = 8;
    public static final byte CLASS_UNLOAD = 9;
    public static final byte CLASS_LOAD = 10;
    public static final byte FIELD_ACCESS = 20;
    public static final byte FIELD_MODIFICATION = 21;
    public static final byte EXCEPTION_CATCH = 30;
    public static final byte METHOD_ENTRY = 40;
    public static final byte METHOD_EXIT = 41;
    public static final byte METHOD_EXIT_WITH_RETURN_VALUE = 42;
    public static final byte MONITOR_CONTENDED_ENTER = 43;
    public static final byte MONITOR_CONTENDED_ENTERED = 44;
    public static final byte MONITOR_WAIT = 45;
    public static final byte MONITOR_WAITED	= 46;
    public static final byte VM_START = 90;
    public static final byte VM_DEATH = 99;
}

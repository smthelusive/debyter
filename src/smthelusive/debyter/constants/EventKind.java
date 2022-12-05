package smthelusive.debyter.constants;

public class EventKind {
    public static final byte EVENT_KIND_BREAKPOINT = 2;
    public static final byte EVENT_KIND_CLASS_LOAD = 10;
    public static final byte EVENT_KIND_CLASS_PREPARE = 8;

    public static final byte EVENT_KIND_SINGLE_STEP = 1;
    public static final byte EVENT_KIND_FRAME_POP = 3;
    public static final byte EVENT_KIND_EXCEPTION = 4;
    public static final byte EVENT_KIND_USER_DEFINED = 5;
    public static final byte EVENT_KIND_THREAD_START = 6;
    public static final byte EVENT_KIND_THREAD_DEATH = 7;
    public static final byte EVENT_KIND_CLASS_UNLOAD = 9;
    public static final byte EVENT_KIND_FIELD_ACCESS = 20;
    public static final byte EVENT_KIND_FIELD_MODIFICATION = 21;
    public static final byte EVENT_KIND_EXCEPTION_CATCH = 30;
    public static final byte EVENT_KIND_METHOD_ENTRY = 40;
    public static final byte EVENT_KIND_METHOD_EXIT = 41;
    public static final byte EVENT_KIND_METHOD_EXIT_WITH_RETURN_VALUE = 42;
    public static final byte EVENT_KIND_MONITOR_CONTENDED_ENTER = 43;
    public static final byte EVENT_KIND_MONITOR_CONTENDED_ENTERED = 44;
    public static final byte EVENT_KIND_MONITOR_WAIT = 45;
    public static final byte EVENT_KIND_MONITOR_WAITED	= 46;
    public static final byte EVENT_KIND_VM_START = 90;
    public static final byte EVENT_KIND_VM_DEATH = 99;
}

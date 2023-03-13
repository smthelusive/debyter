package smthelusive.debyter.constants;

public class Command {
    // Commands set 1:
    public static final int VERSION_CMD = 1;
    public static final int CLASSES_BY_SIGNATURE_CMD = 2;
    public static final int ALL_CLASSES_CMD = 3;
    public static final int ALL_THREADS_CMD = 4;
    public static final int TOP_LEVEL_THREAD_GROUPS_CMD = 5;
    public static final int DISPOSE_CMD = 6;
    public static final int ID_SIZES_CMD = 7;
    public static final int SUSPEND_CMD = 8;
    public static final int RESUME_CMD = 9;
    public static final int EXIT_CMD = 10;
    public static final int CREATE_STRING_CMD = 11;
    public static final int CAPABILITIES_CMD = 12;
    public static final int CLASS_PATHS_CMD = 13;
    public static final int DISPOSE_OBJECTS_CMD = 14;
    public static final int HOLD_EVENTS_CMD = 15;
    public static final int RELEASE_EVENTS_CMD = 16;
    public static final int CAPABILITIES_NEW_CMD = 17;
    public static final int REDEFINE_CLASSES_CMD = 18;
    public static final int SET_DEFAULT_STRATUM_CMD = 19;
    public static final int ALL_CLASSES_WITH_GENERIC_CMD = 20;
    public static final int INSTANCE_COUNTS_CMD = 21;

    // Commands set 2:
    public static final int SIGNATURE_CMD = 1;
    public static final int CLASSLOADER_CMD = 2;
    public static final int MODIFIERS_CMD = 3;
    public static final int FIELDS_CMD = 4;
    public static final int METHODS_CMD = 5;
    public static final int GET_VALUES_CMD = 6;
    public static final int SOURCE_FILE_CMD = 7;
    public static final int NESTED_TYPES_CMD = 8;
    public static final int STATUS_CMD = 9;
    public static final int INTERFACES_CMD = 10;
    public static final int CLASS_OBJECT_CMD = 11;
    public static final int SOURCE_DEBUG_EXTENSION_CMD = 12;
    public static final int SIGNATURE_WITH_GENERIC_CMD = 13;
    public static final int FIELDS_WITH_GENERIC_CMD = 14;
    public static final int METHODS_WITH_GENERIC_CMD = 15;
    public static final int INSTANCES_CMD = 16;
    public static final int CLASS_FILE_VERSION_CMD = 17;
    public static final int CONSTANT_POOL_CMD = 18;

    // Commands set 6:
    public static final int LINETABLE_CMD = 1;
    public static final int VARIABLETABLE_CMD = 2;
    public static final int BYTECODES_CMD = 3;

    // Command set 10:
    public static final int STRING_VALUE_CMD = 1;

    // Command set 11:
    public static final int FRAMES = 6;

    // Command set 13:
    public static final int LENGTH = 1;
    public static final int GET_ARRAY_VALUES = 2;

    // Commands set 15:
    public static final int SET_CMD = 1;
    public static final int CLEAR_CMD = 2;
    public static final int CLEAR_ALL_BREAKPOINTS_CMD = 3;

    // Commands set 16:
    public static final int GET_VARIABLE_VALUES = 1;

    // Commands set 64:
    public static final int COMPOSITE_EVENT_CMD = 100;
}

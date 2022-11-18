package smthelusive.debyter.domain;

public class Constants {
    public static final String LOCALHOST = "127.0.0.1";
    public static final String JDWP_HANDSHAKE = "JDWP-Handshake";
    public static final int EMPTY_FLAGS = 0;

    public static final int EMPTY_PACKET_SIZE = 11;

    public static final int VIRTUAL_MACHINE_COMMAND_SET = 1;
    public static final int REFERENCE_TYPE_COMMAND_SET = 2;
    public static final int CLASS_TYPE_COMMAND_SET = 3;
    public static final int ARRAY_TYPE_COMMAND_SET = 4;
    public static final int EVENT_REQUEST_COMMAND_SET = 15;

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

    // Commands set 3:
    public static final int SUPERCLASS_CMD = 1;
    public static final int SET_VALUES_CMD = 2;
    public static final int INVOKE_METHOD_CMD = 3;
    public static final int CLASS_NEW_INSTANCE_CMD = 4;

    // Commands set 4:
    public static final int ARRAY_NEW_INSTANCE_CMD = 1;

    // Commands set 15:
    public static final int SET_CMD = 1;


    // EVENT KINDS:
    public static final int EVENT_KIND_BREAKPOINT = 2;
    public static final int EVENT_KIND_CLASS_LOAD = 10;

    // RESPONSE TYPES (INTERNAL):
    public static final int RESPONSE_TYPE_NONE = 0;
    public static final int RESPONSE_TYPE_CLASS_INFO = 1;
    public static final int RESPONSE_TYPE_ALL_CLASSES = 2;
    public static final int RESPONSE_TYPE_COMPOSITE_EVENT = 3;

}

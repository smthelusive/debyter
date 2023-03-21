package smthelusive.debyter.constants;

public class Type {
    public static final int ARRAY = 91;
    public static final int BYTE = 66;
    public static final int CHAR = 67;
    public static final int OBJECT = 76;
    public static final int FLOAT = 70;
    public static final int DOUBLE = 68;
    public static final int INT = 73;
    public static final int LONG = 74;
    public static final int SHORT = 83;
    public static final int VOID = 86;
    public static final int BOOLEAN = 90;
    public static final int STRING = 115;
    public static final int THREAD = 116;
    public static final int THREAD_GROUP = 103;
    public static final int CLASS_LOADER = 108;
    public static final int CLASS_OBJECT = 99;

    public static String getTypeName(int typeId) {
        return switch (typeId) {
            case Type.INT -> "int";
            case Type.ARRAY -> "array";
            case Type.STRING -> "String reference";
            case Type.OBJECT -> "object reference";
            case Type.BOOLEAN -> "boolean";
            case Type.BYTE -> "byte";
            case Type.CHAR -> "char";
            case Type.FLOAT -> "float";
            case Type.DOUBLE -> "double";
            case Type.SHORT -> "short";
            case Type.THREAD -> "thread";
            case Type.THREAD_GROUP -> "thread group";
            case Type.CLASS_LOADER -> "class loader";
            case Type.CLASS_OBJECT -> "class object";
            case Type.VOID -> "void";
            case Type.LONG -> "long";
            default -> "unknown";
        };
    }

    public static int getTypeBySignature(String signature) {
        return switch (signature) {
            case "[", "[Ljava/lang/String;" -> ARRAY;
            case "B" -> BYTE;
            case "C" -> CHAR;
            case "F" -> FLOAT;
            case "D" -> DOUBLE;
            case "I" -> INT;
            case "J" -> LONG;
            case "S" -> SHORT;
            case "V" -> VOID;
            case "Z" -> BOOLEAN;
            case "s" -> STRING;
            case "t" -> THREAD;
            case "g" -> THREAD_GROUP;
            case "l" -> CLASS_LOADER;
            case "c" -> CLASS_OBJECT;
            default -> OBJECT;
        };
    }
}

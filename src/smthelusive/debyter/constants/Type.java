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

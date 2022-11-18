package smthelusive.debyter;

public class TheDebuggee {
    public static void main(String[] args) {
        int a = 1 + 1;
        a = a + 15;
        System.out.println(a);
    }
}
// java -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=127.0.0.1:8000 smthelusive/debyter/TheDebuggee

/***
 * Compiled from "TheDebuggee.java"
 * public class smthelusive.debyter.TheDebuggee {
 *   public smthelusive.debyter.TheDebuggee();
 *     Code:
 *        0: aload_0
 *        1: invokespecial #1                  // Method java/lang/Object."<init>":()V
 *        4: return
 *
 *   public static void main(java.lang.String[]);
 *     Code:
 *        0: iconst_2
 *        1: istore_1
 *        2: iload_1
 *        3: bipush        15
 *        5: iadd
 *        6: istore_1
 *        7: getstatic     #7                  // Field java/lang/System.out:Ljava/io/PrintStream;
 *       10: iload_1
 *       11: invokevirtual #13                 // Method java/io/PrintStream.println:(I)V
 *       14: return
 * }
 */
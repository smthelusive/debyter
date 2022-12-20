package smthelusive.debyter;

public class TheDebuggee {
    public static void main(String[] args) {
        int a = 0;
        a += 88;
        System.out.print(a);
        a--;
        a = a * a;
        String test = a + "test";
        System.out.println(test + " hello");
    }
}
// java -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=127.0.0.1:8000 smthelusive/debyter/TheDebuggee
// -Xlog:class+load=info:log.txt
/*
    Compiled from "TheDebuggee.java"
public class smthelusive.debyter.TheDebuggee {a   TheDebuggee.java         UserInputProcessor.java  WIP.class                domain/
public smthelusive.debyter.TheDebuggee();
        Code:
        0: aload_0
        1: invokespecial #1                  // Method java/lang/Object."<init>":()V
        4: return

public static void main(java.lang.String[]);
        Code:
        0: iconst_0
        1: istore_1
        2: iinc          1, 88
        5: getstatic     #7                  // Field java/lang/System.out:Ljava/io/PrintStream;
        8: iload_1
        9: invokevirtual #13                 // Method java/io/PrintStream.print:(I)V
        12: iinc          1, -1
        15: iload_1
        16: iload_1
        17: imul
        18: istore_1
        19: iload_1
        20: invokedynamic #19,  0             // InvokeDynamic #0:makeConcatWithConstants:(I)Ljava/lang/String;
        25: astore_2
        26: getstatic     #7                  // Field java/lang/System.out:Ljava/io/PrintStream;
        29: aload_2
        30: invokedynamic #23,  0             // InvokeDynamic #1:makeConcatWithConstants:(Ljava/lang/String;)Ljava/lang/String;
        35: invokevirtual #26                 // Method java/io/PrintStream.println:(Ljava/lang/String;)V
        38: return
        }

*/

// todo check bytecodes amount for the method with 6-3. if it's 38 or 39 then use it to know when to stop
// todo: parse strings and arrays properly
// todo stop when the last line is met (maybe when the method exit event happens)
// todo test it with WIP file
// todo do a nice logging
// todo make sure that we don't get index out of bounds exception when mapping bytecode to location
// todo parse vm start event
// todo exit doesn't work
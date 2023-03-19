package smthelusive.debyter.examples;

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
// java -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=127.0.0.1:8000 smthelusive/debyter/examples/TheDebuggee
// -Xlog:class+load=info:log.txt
/*
    Compiled from "TheDebuggee.java"
public class smthelusive.debyter.examples.TheDebuggee {a   TheDebuggee.java         UserInputProcessor.java  WIP.class                domain/
public smthelusive.debyter.examples.TheDebuggee();
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



/*
12:11:21.938 [main] INFO smthelusive.debyter.Debyter - Bytecodes received:
12:11:21.943 [main] INFO smthelusive.debyter.Debyter - iconst_0
12:11:21.943 [main] INFO smthelusive.debyter.Debyter - istore_1
12:11:21.943 [main] INFO smthelusive.debyter.Debyter - iinc
12:11:21.943 [main] INFO smthelusive.debyter.Debyter - aconst_null
12:11:21.943 [main] INFO smthelusive.debyter.Debyter - pop2
12:11:21.943 [main] INFO smthelusive.debyter.Debyter - getstatic
12:11:21.943 [main] INFO smthelusive.debyter.Debyter - nop
12:11:21.943 [main] INFO smthelusive.debyter.Debyter - iconst_4
12:11:21.943 [main] INFO smthelusive.debyter.Debyter - iload_1
12:11:21.943 [main] INFO smthelusive.debyter.Debyter - invokevirtual
12:11:21.943 [main] INFO smthelusive.debyter.Debyter - nop
12:11:21.943 [main] INFO smthelusive.debyter.Debyter - fconst_2
12:11:21.943 [main] INFO smthelusive.debyter.Debyter - iinc
12:11:21.943 [main] INFO smthelusive.debyter.Debyter - aconst_null
12:11:21.943 [main] INFO smthelusive.debyter.Debyter - impdep2
12:11:21.943 [main] INFO smthelusive.debyter.Debyter - iload_1
12:11:21.943 [main] INFO smthelusive.debyter.Debyter - iload_1
12:11:21.943 [main] INFO smthelusive.debyter.Debyter - imul
12:11:21.943 [main] INFO smthelusive.debyter.Debyter - istore_1
12:11:21.943 [main] INFO smthelusive.debyter.Debyter - iload_1
12:11:21.944 [main] INFO smthelusive.debyter.Debyter - invokedynamic
12:11:21.944 [main] INFO smthelusive.debyter.Debyter - nop
12:11:21.944 [main] INFO smthelusive.debyter.Debyter - ldc_w
12:11:21.944 [main] INFO smthelusive.debyter.Debyter - nop
12:11:21.944 [main] INFO smthelusive.debyter.Debyter - nop
12:11:21.944 [main] INFO smthelusive.debyter.Debyter - astore_2
12:11:21.944 [main] INFO smthelusive.debyter.Debyter - getstatic
12:11:21.944 [main] INFO smthelusive.debyter.Debyter - nop
12:11:21.944 [main] INFO smthelusive.debyter.Debyter - iconst_4
12:11:21.944 [main] INFO smthelusive.debyter.Debyter - aload_2
12:11:21.944 [main] INFO smthelusive.debyter.Debyter - invokedynamic
12:11:21.944 [main] INFO smthelusive.debyter.Debyter - nop
12:11:21.944 [main] INFO smthelusive.debyter.Debyter - fload
12:11:21.944 [main] INFO smthelusive.debyter.Debyter - nop
12:11:21.944 [main] INFO smthelusive.debyter.Debyter - nop
12:11:21.944 [main] INFO smthelusive.debyter.Debyter - invokevirtual
12:11:21.944 [main] INFO smthelusive.debyter.Debyter - nop
12:11:21.944 [main] INFO smthelusive.debyter.Debyter - iload_0
12:11:21.944 [main] INFO smthelusive.debyter.Debyter - return
 */
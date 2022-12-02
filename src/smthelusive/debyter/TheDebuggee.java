package smthelusive.debyter;

public class TheDebuggee {
    public static void main(String[] args) {
        int a = 0;
        while (true) {
            a++;
            System.out.print(a);
        }
    }
}
// java -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=127.0.0.1:8000 smthelusive/debyter/TheDebuggee
// -Xlog:class+load=info:log.txt

/*
Compiled from "TheDebuggee.java"
public class smthelusive.debyter.TheDebuggee {
  public smthelusive.debyter.TheDebuggee();
    Code:
       0: aload_0
       1: invokespecial #1                  // Method java/lang/Object."<init>":()V
       4: return

  public static void main(java.lang.String[]);
    Code:
       0: iconst_0
       1: istore_1
       2: iinc          1, 1
       5: getstatic     #2                  // Field java/lang/System.out:Ljava/io/PrintStream;
       8: iload_1
       9: invokevirtual #3                  // Method java/io/PrintStream.print:(I)V
      12: goto          2
}

 */
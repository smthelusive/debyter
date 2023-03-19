package smthelusive.debyter.examples;

public class TheDebuggeeWithLoops {
    public static void main(String[] args) {
        int a = 1;
        int c = 0;
        for (int b = 15; b > 0; b--) {
            a += 11;
            c = a + b;
        }
        System.out.println(c);
    }
}

/*
Compiled from "TheDebuggeeWithLoops.java"
public class smthelusive.debyter.TheDebuggeeWithLoops {
  public smthelusive.debyter.TheDebuggeeWithLoops();
    Code:
       0: aload_0
       1: invokespecial #1                  // Method java/lang/Object."<init>":()V
       4: return

  public static void main(java.lang.String[]);
    Code:
       0: iconst_1
       1: istore_1
       2: iconst_0
       3: istore_2
       4: bipush        15
       6: istore_3
       7: iload_3
       8: ifle          24
      11: iinc          1, 11
      14: iload_1
      15: iload_3
      16: iadd
      17: istore_2
      18: iinc          3, -1
      21: goto          7
      24: getstatic     #2                  // Field java/lang/System.out:Ljava/io/PrintStream;
      27: iload_2
      28: invokevirtual #3                  // Method java/io/PrintStream.println:(I)V
      31: return
}

 */

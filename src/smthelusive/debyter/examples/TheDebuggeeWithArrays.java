package smthelusive.debyter.examples;

import java.util.Arrays;

public class TheDebuggeeWithArrays {
    public static void main(String[] args) {
        int a = 88;
        int[] ints = new int[5];
        for (int i = 0; i < ints.length; i++) {
            ints[i] = ints.length - i;
        }
        System.out.println(Arrays.toString(ints));
        a--;
        a = a * a;
        String test = a + "test";
        System.out.println(test);
        String[] strings = new String[2];
        strings[0] = "hello ";
        strings[1] = "world :)";
        System.out.println(strings[0] + strings[1]);
    }
}
/*
Compiled from "TheDebuggeeWithArrays.java"
public class smthelusive.debyter.examples.TheDebuggeeWithArrays {
  public smthelusive.debyter.examples.TheDebuggeeWithArrays();
    Code:
       0: aload_0
       1: invokespecial #1                  // Method java/lang/Object."<init>":()V
       4: return

  public static void main(java.lang.String[]);
    Code:
       0: bipush        88
       2: istore_1
       3: iconst_5
       4: newarray       int
       6: astore_2
       7: iconst_0
       8: istore_3
       9: iload_3
      10: aload_2
      11: arraylength
      12: if_icmpgt     28
      15: aload_2
      16: iload_3
      17: aload_2
      18: arraylength
      19: iload_3
      20: isub
      21: iastore
      22: iinc          3, 1
      25: goto          9
      28: getstatic     #7                  // Field java/lang/System.out:Ljava/io/PrintStream;
      31: aload_2
      32: invokestatic  #13                 // Method java/util/Arrays.toString:([I)Ljava/lang/String;
      35: invokevirtual #19                 // Method java/io/PrintStream.println:(Ljava/lang/String;)V
      38: iinc          1, -1
      41: iload_1
      42: iload_1
      43: imul
      44: istore_1
      45: iload_1
      46: invokedynamic #25,  0             // InvokeDynamic #0:makeConcatWithConstants:(I)Ljava/lang/String;
      51: astore_3
      52: getstatic     #7                  // Field java/lang/System.out:Ljava/io/PrintStream;
      55: aload_3
      56: invokevirtual #19                 // Method java/io/PrintStream.println:(Ljava/lang/String;)V
      59: iconst_2
      60: anewarray     #29                 // class java/lang/String
      63: astore        4
      65: aload         4
      67: iconst_0
      68: ldc           #31                 // String hello
      70: aastore
      71: aload         4
      73: iconst_1
      74: ldc           #33                 // String world :)
      76: aastore
      77: getstatic     #7                  // Field java/lang/System.out:Ljava/io/PrintStream;
      80: aload         4
      82: iconst_0
      83: aaload
      84: aload         4
      86: iconst_1
      87: aaload
      88: invokedynamic #35,  0             // InvokeDynamic #1:makeConcatWithConstants:(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
      93: invokevirtual #19                 // Method java/io/PrintStream.println:(Ljava/lang/String;)V
      96: return
}

 */
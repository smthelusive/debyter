package smthelusive.debyter.examples;

public class TheDebuggeeInfiniteLoop {
    public static void main(String[] args) {
        int a = 1;
        while(true) {
            a += 1;
            a -=1;
        }
    }
}

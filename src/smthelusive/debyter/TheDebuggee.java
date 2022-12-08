package smthelusive.debyter;

public class TheDebuggee {
    public static void main(String[] args) {
        int a = 0;
        a+=88;
        System.out.print(a);
        a--;
        a = a * a;
        String test = a + "test";
        System.out.println(test + " hello");
    }
}
// java -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=127.0.0.1:8000 smthelusive/debyter/TheDebuggee
// -Xlog:class+load=info:log.txt

// todo check bytecodes amount for the method with 6-3. if it's 38 or 39 then use it to know when to stop
// todo: parse strings and arrays properly
// todo stop when the last line is met (maybe when the method exit event happens)
// todo improve logging
// todo test it with WIP file
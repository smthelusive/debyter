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

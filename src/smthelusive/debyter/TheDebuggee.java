package smthelusive.debyter;

public class TheDebuggee {
    public static void main(String[] args) {
        int a = 1 + 1;
        a = a + 15;
        System.out.println(a);
    }
}
// java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=127.0.0.1:8000 smthelusive/debyter/TheDebuggee
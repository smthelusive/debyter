package smthelusive.debyter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

public class Debyter {

    private static Socket clientSocket;
    private static PrintWriter out;
    private static BufferedReader in;

    public static void main(String[] args) {
        startConnection("127.0.0.1", 8000);
        sendMessage("JDWP-Handshake");
        stopConnection();
    }

    public static void startConnection(String ip, int port) {
        try {
            clientSocket = new Socket(ip, port);
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.US_ASCII));
            System.out.println("started successfully");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendMessage(String msg) {
        try {
            System.out.println("sending message...");
            out.println(msg);
            System.out.println("getting response...");
            StringBuilder textBuilder = new StringBuilder();
            int c;
            while ((c = in.read()) > 0) {
                textBuilder.append((char) c);
            }
            System.out.println(textBuilder);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void stopConnection() {
        try {
            in.close();
            out.close();
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
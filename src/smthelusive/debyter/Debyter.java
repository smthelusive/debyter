package smthelusive.debyter;

import smthelusive.debyter.domain.ResponsePacket;
import smthelusive.debyter.domain.Packet;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;


import static smthelusive.debyter.domain.Constants.*;
import static smthelusive.debyter.Utils.*;

public class Debyter implements ResponseListener {

    private static Socket clientSocket;
    private static OutputStream out;
    private static InputStream in;

    private static boolean keepProcessing = true;

    private static int id = 0;
    private static ResponseProcessor responseProcessor;

    private static int getNewUniqueId() {
        return id++;
    }

    public static void main(String[] args) throws Exception {
        startConnection(LOCALHOST, 8000);
        jdwpHandshake();
        responseProcessor = new ResponseProcessor(in);
        Debyter debyter = new Debyter(); // todo if this works, this is ugly so needs to be refactored in a separate place
        responseProcessor.addListener(debyter);
        responseProcessor.start();
        // get info about classes to get a classID
        // check capabilities:
//        sendEmptyPacket(getNewUniqueId(),
//                EMPTY_FLAGS,
//                VIRTUAL_MACHINE_COMMAND_SET,
//                CAPABILITIES_NEW_CMD,
//                NO_RESPONSE_EXPECTED
//                );
        // boolean	canUseSourceNameFilters	Can the VM filter class prepare events by source name? NO
        // this capability is not possible for me

        sendEmptyPacket(getNewUniqueId(),
                EMPTY_FLAGS,
                VIRTUAL_MACHINE_COMMAND_SET,
                ALL_CLASSES_CMD,
                RESPONSE_TYPE_ALL_CLASSES); // success
        requestClassDataBySignature(
                getNewUniqueId(),
                EMPTY_FLAGS,
                VIRTUAL_MACHINE_COMMAND_SET,
                CLASSES_BY_SIGNATURE_CMD,
                "Lsmthelusive/debyter/TheDebuggee;",
                RESPONSE_TYPE_CLASS_INFO); // success, empty
        // get info about methods in the class to get a methodID
        requestClassLoadEvent("smthelusive.debyter.TheDebuggee");
        sendEmptyPacket(getNewUniqueId(), EMPTY_FLAGS, VIRTUAL_MACHINE_COMMAND_SET, RESUME_CMD, RESPONSE_TYPE_NONE);

        keepProcessing = false;
    }

    public static void sendEmptyPacket(int id, int flags, int commandSet,
                                       int command, int responseType) throws Exception {
        sendHeader(EMPTY_PACKET_SIZE, id, flags, commandSet, command, responseType);
        out.flush();
    }

    private static void sendHeader(int length, int id, int flags, int commandSet,
                                   int command, int responseType) throws Exception {
        if (responseType != 0) responseProcessor.requestIsSent(id, responseType);
        out.write(getBytesOfInt(length));
        out.write(getBytesOfInt(id));
        out.write(getByteOfInt(flags));
        out.write(getByteOfInt(commandSet));
        out.write(getByteOfInt(command));
    }

    public static void requestClassDataBySignature(int id, int flags, int commandSet,
                                                   int command, String signature, int responseType) throws Exception {
        byte[] signatureBytes = signature.getBytes(StandardCharsets.US_ASCII);
        sendHeader(EMPTY_PACKET_SIZE + signatureBytes.length + 4, id, flags, commandSet, command, responseType);
        out.write(Utils.getBytesOfInt(signatureBytes.length));
        out.write(signatureBytes);
        out.flush();
    }

    // todo use different way to request class load event
    public static void requestClassLoadEvent(String className) throws Exception {
        int id = getNewUniqueId();
        responseProcessor.requestIsSent(id, RESPONSE_TYPE_NONE); // todo
        Packet packet = new Packet(id, EMPTY_FLAGS, EVENT_REQUEST_COMMAND_SET, SET_CMD);
        packet.addDataAsByte(EVENT_KIND_CLASS_LOAD);
        packet.addDataAsByte(2); // suspendPolicy: suspend all
        packet.addDataAsInt(1); // modifiers
        packet.addDataAsByte(5); // modKind: ClassMatch
        // classPattern string:
        byte[] classNameBytes = className.getBytes();
        packet.addDataAsBytes(Utils.getBytesOfInt(classNameBytes.length));
        packet.addDataAsBytes(classNameBytes);
        out.write(packet.getPacketBytes());
        out.flush();
    }

    public static void startConnection(String ip, int port) {
        try {
            clientSocket = new Socket(ip, port);
            out = clientSocket.getOutputStream();
            in = clientSocket.getInputStream();
            System.out.println("started successfully");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void jdwpHandshake() {
        try {
            System.out.println("sending message...");
            out.write(JDWP_HANDSHAKE.getBytes(StandardCharsets.US_ASCII));
            System.out.println("getting response...");
            byte[] result = in.readNBytes(14);
            System.out.println(new String(result, StandardCharsets.US_ASCII));
            in.readNBytes(29); // it's probably VM started event, todo accept/parse later
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

    public static void setBreakPoint(int location) throws Exception {
        int id = getNewUniqueId();
        responseProcessor.requestIsSent(id, 0); // todo type
        Packet packet = new Packet(id, EMPTY_FLAGS, EVENT_REQUEST_COMMAND_SET, SET_CMD);
        packet.addDataAsByte(EVENT_KIND_BREAKPOINT);
        packet.addDataAsByte(2); // suspendPolicy: suspend all
        packet.addDataAsInt(1); // modifiers
        packet.addDataAsByte(7); // modKind: LocationOnly
        /*
        Location:

        tag (1 byte)
        CLASS = 1;
        INTERFACE = 2;
        ARRAY = 3;

        classRef (short | int | long) up to 8 bytes
        methodRef (short | int | long) up to 8 bytes
        codeIndex (long)

         */

        packet.addDataAsInt(location);
        out.write(packet.getPacketBytes());
        out.flush();
    }

    @Override
    public void incomingPacket(ResponsePacket incomingPacket) {
        System.out.println(incomingPacket);
    }

    @Override
    public void finish() {
        if (!keepProcessing) {
            responseProcessor.finishProcessing();
            stopConnection();
        }
    }
}

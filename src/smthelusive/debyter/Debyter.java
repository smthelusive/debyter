package smthelusive.debyter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smthelusive.debyter.constants.*;
import smthelusive.debyter.domain.*;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;


import static smthelusive.debyter.constants.Command.*;
import static smthelusive.debyter.constants.CommandSet.*;
import static smthelusive.debyter.constants.EventKind.*;
import static smthelusive.debyter.constants.ResponseType.*;
import static smthelusive.debyter.constants.Constants.*;
import static smthelusive.debyter.Utils.*;

public class Debyter implements ResponseListener {

    private static Socket clientSocket;
    private static OutputStream out;
    private static InputStream in;
    private long classId;
    private long threadId;

    private VariableTable variableTable;
    private LineTable lineTable;
    private Location location;
    private byte[] bytecodes;
    private List<AMethod> methods;
    private static boolean keepProcessing = true;
    private static final Logger logger = LoggerFactory.getLogger(Debyter.class);
    private final Scanner sc = new Scanner(System.in);

    private static int id = 0;
    private static ResponseProcessor responseProcessor;

    private static int getNewUniqueId() {
        return id++;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            logger.error("wrong amount of incoming parameters. " +
                    "please specify the full class name");
            return;
        }
        startConnection(LOCALHOST, PORT);
        jdwpHandshake();
        ResponseNotifier notifier = new ResponseNotifier();
        Debyter debyter = new Debyter();
        notifier.addListener(debyter);
        responseProcessor = new ResponseProcessor(in, notifier);
        responseProcessor.start();
        requestClassPrepareEvent(args[0]);
    }

    public static void exit() {
        // keepProcessing = false;
        // todo request VM death event
    }

    public static void requestByteCodes(long classId, long methodId) {
        try {
            int id = getNewUniqueId();
            responseProcessor.requestIsSent(id, RESPONSE_TYPE_BYTECODES);
            Packet packet = new Packet(id, EMPTY_FLAGS, METHOD_COMMAND_SET, BYTECODES_CMD);
            packet.addDataAsLong(classId);
            packet.addDataAsLong(methodId);
            out.write(packet.getPacketBytes());
            out.flush();
        } catch (Exception e) {
            logger.error("something went wrong during requesting the bytecodes");
        }
    }

    public static void classInfo(String signature) throws Exception {
        requestClassDataBySignature(
        getNewUniqueId(),
        EMPTY_FLAGS,
        VIRTUAL_MACHINE_COMMAND_SET,
        CLASSES_BY_SIGNATURE_CMD,
        signature,
        RESPONSE_TYPE_CLASS_INFO);
    }

    public static void idSizes() throws Exception {
        sendEmptyPacket(getNewUniqueId(),
                EMPTY_FLAGS,
                VIRTUAL_MACHINE_COMMAND_SET,
                ID_SIZES_CMD,
                RESPONSE_TYPE_ID_SIZES); // success
    }

    public static void allClasses() throws Exception {
        sendEmptyPacket(getNewUniqueId(),
        EMPTY_FLAGS,
        VIRTUAL_MACHINE_COMMAND_SET,
        ALL_CLASSES_CMD,
        RESPONSE_TYPE_ALL_CLASSES); // success
    }

    public static void capabilitiesNew() throws Exception{
        sendEmptyPacket(getNewUniqueId(),
        EMPTY_FLAGS,
        VIRTUAL_MACHINE_COMMAND_SET,
        CAPABILITIES_NEW_CMD,
        RESPONSE_TYPE_NONE
        );
    }

    public void resume() {
        try {
            sendEmptyPacket(getNewUniqueId(), EMPTY_FLAGS, VIRTUAL_MACHINE_COMMAND_SET, RESUME_CMD, RESPONSE_TYPE_NONE);
        } catch (Exception e) {
            logger.error("something went wrong during resuming the JVM");
        }
        listenUserInput();
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
        byte[] signatureBytes = signature.getBytes();
        sendHeader(EMPTY_PACKET_SIZE + signatureBytes.length + INTEGER_LENGTH_BYTES,
                id, flags, commandSet, command, responseType);
        out.write(Utils.getBytesOfInt(signatureBytes.length));
        out.write(signatureBytes);
        out.flush();
    }

    public static void requestClassPrepareEvent(String className) throws Exception {
        int id = getNewUniqueId();
        responseProcessor.requestIsSent(id, RESPONSE_TYPE_COMPOSITE_EVENT);
        Packet packet = new Packet(id, EMPTY_FLAGS, EVENT_REQUEST_COMMAND_SET, SET_CMD);
        packet.addDataAsByte(EVENT_KIND_CLASS_PREPARE);
        packet.addDataAsByte(SuspendPolicy.ALL);
        packet.addDataAsInt(1); // modifiers
        packet.addDataAsByte(ModKind.CLASS_MATCH);

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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void jdwpHandshake() {
        try {
            out.write(JDWP_HANDSHAKE.getBytes(StandardCharsets.US_ASCII));
            byte[] result = in.readNBytes(HANDSHAKE_SIZE);
            logger.info(new String(result, StandardCharsets.US_ASCII));
            in.readNBytes(29); // it's probably VM started event, todo accept/parse later
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    public static void stopConnection() {
        try {
            in.close();
            out.close();
            clientSocket.close();
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    public static void requestBreakpointEvent(long classId, long methodId, long codeIndex) {
        try {
            int id = getNewUniqueId();
            responseProcessor.requestIsSent(id, RESPONSE_TYPE_COMPOSITE_EVENT);
            Packet packet = new Packet(id, EMPTY_FLAGS, EVENT_REQUEST_COMMAND_SET, SET_CMD);
            packet.addDataAsByte(EVENT_KIND_BREAKPOINT);
            packet.addDataAsByte(SuspendPolicy.ALL);
            packet.addDataAsInt(1); // modifiers
            packet.addDataAsByte(ModKind.LOCATION_ONLY);
            packet.addDataAsByte(TypeTag.CLASS);
            packet.addDataAsLong(classId);
            packet.addDataAsLong(methodId);
            packet.addDataAsLong(codeIndex);
            out.write(packet.getPacketBytes());
            out.flush();
        } catch (Exception e) {
            logger.error("something went wrong during setting the breakpoint");
        }
    }

    public void requestStepOverEvent() {
        try {
            int id = getNewUniqueId();
            responseProcessor.requestIsSent(id, RESPONSE_TYPE_SINGLE_STEP);
            Packet packet = new Packet(id, EMPTY_FLAGS, EVENT_REQUEST_COMMAND_SET, SET_CMD);
            packet.addDataAsByte(EVENT_KIND_SINGLE_STEP);
            packet.addDataAsByte(SuspendPolicy.ALL);
            packet.addDataAsInt(1); // modifiers
            packet.addDataAsByte(ModKind.STEP);
            packet.addDataAsLong(threadId);
            packet.addDataAsInt(Step.STEP_MIN);
            packet.addDataAsInt(Step.STEP_OVER);
            out.write(packet.getPacketBytes());
            out.flush();
        } catch (Exception e) {
            logger.error("something went wrong during requesting step over");
        }
    }

    public void clear() {
        // todo implement


        listenUserInput();
    }

    private void requestMethodsOfClassInfo(long typeID) {
        int id = getNewUniqueId();
        responseProcessor.requestIsSent(id, RESPONSE_TYPE_METHODS);
        Packet packet = new Packet(id, EMPTY_FLAGS, REFERENCE_TYPE_COMMAND_SET, METHODS_CMD);
        packet.addDataAsLong(typeID);
        try {
            out.write(packet.getPacketBytes());
            out.flush();
        } catch (IOException ioe) {
            logger.error(ioe.getMessage());
        }
    }

    private void requestCurrentFrameInfo(long threadId) {
        int id = getNewUniqueId();
        responseProcessor.requestIsSent(id, RESPONSE_TYPE_FRAME_INFO);
        Packet packet = new Packet(id, EMPTY_FLAGS, THREAD_REFERENCE_COMMAND_SET, FRAMES);
        packet.addDataAsLong(threadId);
        packet.addDataAsInt(0); // current frame
        packet.addDataAsInt(1); // amount of frames to retrieve
        try {
            out.write(packet.getPacketBytes());
            out.flush();
        } catch (IOException ioe) {
            logger.error(ioe.getMessage());
        }
    }

    private void requestLocalVariables(long threadId, long frameId, List<Variable> variables) {
        int id = getNewUniqueId();
        responseProcessor.requestIsSent(id, RESPONSE_TYPE_LOCAL_VARIABLES);
        Packet packet = new Packet(id, EMPTY_FLAGS, STACK_FRAME_COMMAND_SET, GET_VALUES);
        packet.addDataAsLong(threadId);
        packet.addDataAsLong(frameId);
        packet.addDataAsInt(variables.size()); // amount of variables
        for (Variable variable: variables) {
            packet.addDataAsInt(variable.slot()); // index in locals array
            packet.addDataAsByte(Type.getTypeBySignature(variable.signature()));
        }
        try {
            out.write(packet.getPacketBytes());
            out.flush();
        } catch (IOException ioe) {
            logger.error(ioe.getMessage());
        }
    }

    /*
    Either line table or variable table
     */
    private void requestTableInfo(long classId, long methodId, int cmd) {
        try {
            int responseType = cmd == LINETABLE_CMD ? RESPONSE_TYPE_LINETABLE : RESPONSE_TYPE_VARIABLETABLE;
            int id = getNewUniqueId();
            responseProcessor.requestIsSent(id, responseType);
            Packet packet = new Packet(id, EMPTY_FLAGS, METHOD_COMMAND_SET, cmd);
            packet.addDataAsLong(classId);
            packet.addDataAsLong(methodId);
            out.write(packet.getPacketBytes());
            out.flush();
        } catch (Exception e) {
            logger.error("something went wrong during requesting the table: " + e.getMessage());
        }
    }

    /*
    this returns info to be able to map bytecode operation indices to java code line numbers
     */
    private void requestLineTableInfo(long classId, long methodId) {
        requestTableInfo(classId, methodId, LINETABLE_CMD);
    }

    private void requestVariableTableInfo(long classId, long methodId) {
        requestTableInfo(classId, methodId, VARIABLETABLE_CMD);
    }

    @Override
    public void incomingPacket(ResponsePacket incomingPacket) {
//        logger.info(incomingPacket.toString());
    }

    @Override
    public void finish() {
        if (!keepProcessing) {
            responseProcessor.finishProcessing();
            stopConnection();
        }
    }

    @Override
    public void classIsLoaded(long refTypeId) {
        try {
            requestMethodsOfClassInfo(refTypeId);
        } catch (Exception e) {
            logger.error("something went wrong during obtaining methods info");
        }
    }

    private void listenUserInput() {
        String[] command = sc.nextLine().split("\\s");
        switch (command[0]) {
            case "bp":
                if (command.length < 3) {
                    logger.error("please specify method name and code index to set a breakpoint");
                } else {
                    String methodName = command[1];
                    long methodId = methods.stream()
                            .filter(method -> method.name().equals(methodName))
                            .findAny().map(AMethod::methodId).orElse(0L);
                    int codeIndex = Integer.parseInt(command[2]);
                    requestLineTableInfo(classId, methodId);
                    requestByteCodes(classId, methodId);
                    requestVariableTableInfo(classId, methodId);
                    requestBreakpointEvent(classId, methodId, codeIndex);
                }
                break;
            case "step":
                logger.info("step over...");
                requestStepOverEvent();
                requestClearStepEvents();
            case "resume":
                logger.info("resuming...");
                resume();
                break;
            case "exit":
                logger.info("exiting...");
                exit();
                break;
            case "clear":
                logger.info("clearing breakpoints...");
                requestClearBreakpoints();

        }
    }

    public void requestClearStepEvents() {
        // todo
        listenUserInput();
    }

    public void requestClearBreakpoints() {
        // todo
        listenUserInput();
    }

    @Override
    public void classAndMethodsInfoObtained(long threadId, long classId, List<AMethod> methods) {
        this.threadId = threadId;
        this.classId = classId;
        this.methods = methods;
        logger.info("class is ready to be debugged. please enter command");
        listenUserInput();
    }

    @Override
    public void breakPointHit(long threadId, Location location) {
        logger.info("BREAKPOINT HIT. current bytecode operation: " +
                BYTECODE_OPERATIONS.get(bytecodes[(int)location.codeIndex()])
                + ", line #" + location.codeIndex());
        this.location = location;
        requestCurrentFrameInfo(threadId);
    }

    @Override
    public void frameIdObtained(long frameId) {
        List<Variable> visibleVariables = variableTable.getVariables().stream()
                .filter(variable -> location.codeIndex() > variable.codeIndex() &&
                        location.codeIndex() < variable.codeIndex() + variable.length()).toList();
        requestLocalVariables(threadId, frameId, visibleVariables);
    }

    @Override
    public void lineTableObtained(LineTable lineTable) {
        this.lineTable = lineTable;
    }

    @Override
    public void variableTableObtained(VariableTable variableTable) {
        this.variableTable = variableTable;
    }

    @Override
    public void variablesReceived() {
        listenUserInput();
    }

    @Override
    public void bytecodesReceived(byte[] bytecodes) {
        this.bytecodes = bytecodes;
    }
}
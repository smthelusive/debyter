package smthelusive.debyter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smthelusive.debyter.constants.*;
import smthelusive.debyter.domain.*;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


import static smthelusive.debyter.constants.Command.*;
import static smthelusive.debyter.constants.CommandSet.*;
import static smthelusive.debyter.constants.EventKind.*;
import static smthelusive.debyter.constants.Constants.*;
import static smthelusive.debyter.Utils.*;
import static smthelusive.debyter.constants.ResponseType.*;

public class Debyter implements ResponseListener, UserInputListener {

    private static Socket clientSocket;
    private static OutputStream out;
    private static InputStream in;
    private static long classId;
    private static long threadId;

    private static final BlockingQueue<ResponsePacket> responsePackets = new LinkedBlockingQueue<>();
    private static final BlockingQueue<UserCommand> userCommands = new LinkedBlockingQueue<>();

    private static VariableTable variableTable;
    private static LineTable lineTable;
    private static Location location;
    private static byte[] bytecodes;
    private static ResponseProcessor responseProcessor;
    private static UserInputProcessor userInputProcessor;
    private static List<AMethod> methods;
    private static boolean keepProcessing = true;
    private static final Logger logger = LoggerFactory.getLogger(Debyter.class);
    private static int id = 0;
    private static boolean userCanInteract = false;
    private static boolean stepping = false;

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
        Debyter debyter = new Debyter();
        userInputProcessor = new UserInputProcessor(debyter);
        userInputProcessor.start();
        responseProcessor = new ResponseProcessor(in, debyter);
        responseProcessor.start();
        requestClassPrepareEvent(args[0]);
        while (keepProcessing) {
            processAllResponsePackets();
            if (userCanInteract) processAUserCommand();
        }
    }

    private static void processAUserCommand() {
        if (!userCommands.isEmpty()) {
            UserCommand userCommand = userCommands.poll();
            switch (userCommand.userCommandType()) {
                case BREAKPOINT -> {
                    if (userCommand.params().length != 2)
                        logger.error("please specify method name and line number to set the breakpoint");
                    else {
                        userRequestBreakpoint(
                                userCommand.params()[0],
                                Integer.parseInt(userCommand.params()[1]));
                    }
                }
                case STEP_OVER -> {
                    if (!stepping) {
                        requestStepOverEvent();
                        stepping = true;
                    }
                }
                case CLEAR -> userRequestClearBreakpoints();
                case RESUME -> requestResume();
                case EXIT -> requestVMDeathEvent();
            }
        }
    }

    private static void processAllResponsePackets() {
        List<ResponsePacket> batch = new ArrayList<>();
        responsePackets.drainTo(batch);
        batch.forEach(responsePacket -> {
            switch (responsePacket.getResponseType()) {
                case RESPONSE_TYPE_COMPOSITE_EVENT -> responsePacket.getEventsList().forEach(Debyter::processEvent);
                case RESPONSE_TYPE_FRAME_INFO -> frameIdObtained(responsePacket.getFrameId());
                case RESPONSE_TYPE_BYTECODES -> bytecodes = responsePacket.getBytecodes();
                case RESPONSE_TYPE_VARIABLETABLE -> variableTable = responsePacket.getVariableTable();
                case RESPONSE_TYPE_LINETABLE -> lineTable = responsePacket.getLineTable();
                case RESPONSE_TYPE_LOCAL_VARIABLES -> variablesReceived(responsePacket.getGenericVariables());
                case RESPONSE_TYPE_CLASS_INFO -> {}
                case RESPONSE_TYPE_EVENT_REQUEST -> logger.info("event request registered");
                case RESPONSE_TYPE_METHODS -> methodsInfoObtained(responsePacket.getMethods());
                case RESPONSE_TYPE_STRING_VALUE -> logger.info("String value: " + responsePacket.getStringValue());
            }
        });
    }

    private static void processEvent(Event event) {
        switch (event.getInternalEventType()) {
            case VM_DEATH -> vmDeathEventReceived();
            case BREAKPOINT_HIT -> {
                location = event.getLocation();
                breakPointHit(event.getThread(), location);
            }
            case STEP_HIT -> {
                if (stepping) {
                    location = event.getLocation();
                    breakPointHit(event.getThread(), location);
                    stepping = false;
                }
            }
            case CLASS_LOADED -> {
                threadId = event.getThread();
                classIsLoaded(event.getRefTypeId());
            }
        }
    }

    private static void requestByteCodes(long classId, long methodId) {
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

    private static void idSizes() throws Exception {
        sendEmptyPacket(getNewUniqueId(),
                VIRTUAL_MACHINE_COMMAND_SET,
                ID_SIZES_CMD,
                RESPONSE_TYPE_ID_SIZES);
    }

    private static void allClasses() throws Exception {
        sendEmptyPacket(getNewUniqueId(),
        VIRTUAL_MACHINE_COMMAND_SET,
        ALL_CLASSES_CMD,
        RESPONSE_TYPE_ALL_CLASSES);
    }

    private static void capabilitiesNew() throws Exception {
        sendEmptyPacket(getNewUniqueId(),
        VIRTUAL_MACHINE_COMMAND_SET,
        CAPABILITIES_NEW_CMD,
        RESPONSE_TYPE_NONE);
    }

    private static void sendEmptyPacket(int id, int commandSet, int command, int responseType) throws Exception {
        sendHeader(EMPTY_PACKET_SIZE, id, EMPTY_FLAGS, commandSet, command, responseType);
        out.flush();
    }

    private static void sendHeader(int length, int id, int flags, int commandSet,
                                   int command, int responseType) throws Exception {
        if (responseType != RESPONSE_TYPE_NONE) responseProcessor.requestIsSent(id, responseType);
        out.write(getBytesOfInt(length));
        out.write(getBytesOfInt(id));
        out.write(getByteOfInt(flags));
        out.write(getByteOfInt(commandSet));
        out.write(getByteOfInt(command));
    }

    private static void requestStringValue(long stringId) {
        try {
            Packet packet = new Packet(getNewUniqueId(), EMPTY_FLAGS, STRING_REFERENCE_COMMAND_SET, STRING_VALUE_CMD);
            packet.addDataAsLong(stringId);
            out.write(packet.getPacketBytes());
            out.flush();
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    private static void requestClassDataBySignature(String signature) throws Exception {
        byte[] signatureBytes = signature.getBytes(); // todo check doesn't int size of string have to be included in the total size?
        sendHeader(EMPTY_PACKET_SIZE + signatureBytes.length + INTEGER_LENGTH_BYTES,
                getNewUniqueId(), EMPTY_FLAGS, VIRTUAL_MACHINE_COMMAND_SET, CLASSES_BY_SIGNATURE_CMD, RESPONSE_TYPE_CLASS_INFO);
        out.write(Utils.getBytesOfInt(signatureBytes.length));
        out.write(signatureBytes);
        out.flush();
    }

    private static void requestClassPrepareEvent(String className) throws Exception {
        int id = getNewUniqueId();
        responseProcessor.requestIsSent(id, RESPONSE_TYPE_COMPOSITE_EVENT);
        Packet packet = new Packet(id, EMPTY_FLAGS, EVENT_REQUEST_COMMAND_SET, SET_CMD);
        packet.addDataAsByte(EVENT_KIND_CLASS_PREPARE);
        packet.addDataAsByte(SuspendPolicy.ALL);
        packet.addDataAsInt(1); // modifiers
//        packet.addDataAsByte(ModKind.COUNT);
//        packet.addDataAsInt(1);
        packet.addDataAsByte(ModKind.CLASS_MATCH);
        byte[] classNameBytes = className.getBytes();
        packet.addDataAsBytes(Utils.getBytesOfInt(classNameBytes.length));
        packet.addDataAsBytes(classNameBytes);
        out.write(packet.getPacketBytes());
        out.flush();
        requestResume();
    }

    private static void startConnection(String ip, int port) {
        try {
            clientSocket = new Socket(ip, port);
            out = clientSocket.getOutputStream();
            in = clientSocket.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void jdwpHandshake() {
        try {
            out.write(JDWP_HANDSHAKE.getBytes(StandardCharsets.US_ASCII));
            byte[] result = in.readNBytes(HANDSHAKE_SIZE);
            logger.info(new String(result, StandardCharsets.US_ASCII));
            in.readNBytes(29); // it's probably VM started event, todo accept/parse later
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    private static void stopConnection() {
        try {
            in.close();
            out.close();
            clientSocket.close();
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    private static void userRequestBreakpoint(String methodName, long codeIndex) {
        long methodId = methods.stream()
                .filter(method -> method.name().equals(methodName))
                .findAny().map(AMethod::methodId).orElse(0L);
        requestLineTableInfo(classId, methodId);
        requestByteCodes(classId, methodId);
        requestVariableTableInfo(classId, methodId);
        requestBreakpointEvent(classId, methodId, codeIndex);
    }

    private static void requestBreakpointEvent(long classId, long methodId, long codeIndex) {
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

    private static void requestVMDeathEvent() {
        try {
            int id = getNewUniqueId();
            responseProcessor.requestIsSent(id, RESPONSE_TYPE_COMPOSITE_EVENT);
            Packet packet = new Packet(id, EMPTY_FLAGS, EVENT_REQUEST_COMMAND_SET, SET_CMD);
            packet.addDataAsByte(EVENT_KIND_VM_DEATH);
            packet.addDataAsByte(SuspendPolicy.NONE);
            packet.addDataAsInt(0);
            out.write(packet.getPacketBytes());
            out.flush();
        } catch (Exception e) {
            logger.error("something went wrong during requesting vm death event");
        }
    }

    private static void requestStepOverEvent() {
        try {
            int id = getNewUniqueId();
            responseProcessor.requestIsSent(id, RESPONSE_TYPE_SINGLE_STEP);
            Packet packet = new Packet(id, EMPTY_FLAGS, EVENT_REQUEST_COMMAND_SET, SET_CMD);
            packet.addDataAsByte(EVENT_KIND_SINGLE_STEP);
            packet.addDataAsByte(SuspendPolicy.ALL);
            packet.addDataAsInt(1); // modifiers
//            packet.addDataAsByte(ModKind.COUNT);
//            packet.addDataAsInt(1);
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

    private static void requestMethodsOfClassInfo(long typeID) {
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

    private static void requestCurrentFrameInfo(long threadId) {
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

    private static void requestLocalVariables(long threadId, long frameId, List<Variable> variables) {
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
    private static void requestTableInfo(long classId, long methodId, int cmd) {
        try {
            int responseType = cmd ==
                    LINETABLE_CMD ? RESPONSE_TYPE_LINETABLE : RESPONSE_TYPE_VARIABLETABLE;
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
    private static void requestLineTableInfo(long classId, long methodId) {
        requestTableInfo(classId, methodId, LINETABLE_CMD);
    }

    private static void requestVariableTableInfo(long classId, long methodId) {
        requestTableInfo(classId, methodId, VARIABLETABLE_CMD);
    }

    private static void requestResume() {
        try {
            sendEmptyPacket(getNewUniqueId(), VIRTUAL_MACHINE_COMMAND_SET, RESUME_CMD, RESPONSE_TYPE_NONE);
        } catch (Exception e) {
            logger.error("something went wrong during resuming the JVM");
        }
    }

    private static void userRequestClearBreakpoints() {
        try {
            sendEmptyPacket(getNewUniqueId(),
                    EVENT_REQUEST_COMMAND_SET,
                    CLEAR_ALL_BREAKPOINTS_CMD,
                    RESPONSE_TYPE_NONE);
        } catch (Exception e) {
            logger.error(e.toString());
        }
    }

    private static void vmDeathEventReceived() {
        responseProcessor.finishProcessing();
        userInputProcessor.finishProcessing();
        stopConnection();
        keepProcessing = false;
    }

    private static void classIsLoaded(long refTypeId) {
        classId = refTypeId;
        try {
            requestMethodsOfClassInfo(refTypeId);
        } catch (Exception e) {
            logger.error("something went wrong during obtaining methods info");
        }
    }

    private static void methodsInfoObtained(List<AMethod> methodList) {
        methods = methodList;
        userCanInteract = true;
        logger.info("class is ready to be debugged. please enter command");
    }

    private static void breakPointHit(long threadId, Location location) {
        logger.info("BREAKPOINT HIT. current bytecode operation: " +
                BYTECODE_OPERATIONS.get(bytecodes[(int)location.codeIndex()])
                + ", line #" + location.codeIndex());
        requestCurrentFrameInfo(threadId);
    }

    private static void frameIdObtained(long frameId) {
        List<Variable> visibleVariables = variableTable.getVariables().stream()
                .filter(variable -> location.codeIndex() > variable.codeIndex() &&
                        location.codeIndex() < variable.codeIndex() + variable.length()).toList();
        requestLocalVariables(threadId, frameId, visibleVariables);
    }

    private static void variablesReceived(List<GenericVariable> variables) {
        logger.info("LOCAL VARIABLES:");
        for (GenericVariable variable: variables) {
            switch (variable.type()) {
                case Type.INT -> logger.info("received int, value: " + variable.value());
                case Type.ARRAY -> {
                    logger.info("received array, reference: " + variable.value());
                }
                case Type.STRING -> {
                    requestStringValue(variable.value());
                    logger.info("received array, reference: " + variable.value());
                }
                case Type.OBJECT -> {
                    logger.info("received object, reference: " + variable.value());
                }
                default -> logger.error("something unexpected received");
            }
            logger.info(variable.toString());
        }
    }

    @Override
    public void incomingPacket(ResponsePacket incomingPacket) {
        responsePackets.add(incomingPacket);
    }

    @Override
    public void addEventToTheQueue(UserCommand userCommand) {
        userCommands.add(userCommand);
    }
}
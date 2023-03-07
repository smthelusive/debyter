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

// todo rethink the flow of response processing...
// todo set breakpoints using class names together with method names and code index
public class Debyter implements ResponseListener, UserInputListener {

    private static Socket clientSocket;
    private static OutputStream out;
    private static InputStream in;
    private static final BlockingQueue<ResponsePacket> responsePackets = new LinkedBlockingQueue<>();
    private static final BlockingQueue<UserCommand> userCommands = new LinkedBlockingQueue<>();
    private static CurrentState currentState = new CurrentState(); // todo this is not very nice
    private static ResponseProcessor responseProcessor;
    private static UserInputProcessor userInputProcessor;
    private static boolean keepProcessing = true;
    private static final Logger logger = LoggerFactory.getLogger(Debyter.class);
    private static int id = 0;
    private static boolean userCanInteract = false;

    private static int getNewUniqueId() {
        return id++;
    }

    public static void main(String[] args) throws Exception { // todo no classname arguments
        if (args.length != 1) {
            logger.error("wrong amount of incoming parameters. " +
                    "please specify the full class name");
            return;
        }
        startConnection(LOCALHOST, PORT);
        jdwpHandshake();
        Debyter debyter = new Debyter();
        userInputProcessor = new UserInputProcessor();
        userInputProcessor.addListener(debyter);
        userInputProcessor.start();
        responseProcessor = new ResponseProcessor(in);
        responseProcessor.addListener(debyter);
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
                case STEP_OVER -> requestStepOverEvents();
                case CLEAR -> userRequestClearBreakpoints();
                case RESUME -> requestResume();
                case EXIT -> requestExit();
            }
        }
    }

    private static void processAllResponsePackets() {
        List<ResponsePacket> batch = new ArrayList<>();
        responsePackets.drainTo(batch);
        batch.forEach(responsePacket -> {
            switch (responsePacket.getResponseType()) {
                case RESPONSE_TYPE_COMPOSITE_EVENT -> responsePacket.getEventsList().forEach(Debyter::processEvent);
                case RESPONSE_TYPE_FRAME_INFO -> requestLocalVariables(responsePacket.getFrameId());
                case RESPONSE_TYPE_BYTECODES -> {
                    currentState.setBytecodes(responsePacket.getBytecodes());


                }
                case RESPONSE_TYPE_VARIABLETABLE -> currentState.setVariableTable(responsePacket.getVariableTable());
                case RESPONSE_TYPE_LINETABLE -> logger.info("Linetable received: " + responsePacket.getLineTable());
                case RESPONSE_TYPE_LOCAL_VARIABLES -> {
                    logCurrentMethod();
                    logCurrentOperation();
                    logLocalVariables(responsePacket.getGenericVariables());
                }
                case RESPONSE_TYPE_CLASS_INFO -> {} // todo currentState.setClassId(responsePacket.getClassId());
                case RESPONSE_TYPE_EVENT_REQUEST -> logger.info("event request registered");
                case RESPONSE_TYPE_METHODS -> methodsInfoObtained(responsePacket.getMethods());
                case RESPONSE_TYPE_STRING_VALUE -> logger.info("String, value: " + responsePacket.getStringValue());
            }
        });
    }

    private static void processEvent(Event event) {
        switch (event.getInternalEventType()) {
            case VM_START -> logger.info("VM started");
            case VM_DEATH -> finishAndStop();
            case BREAKPOINT_HIT -> {
                logger.info("BREAKPOINT HIT line #" + event.getLocation().codeIndex());
                processHitLocation(event.getThread(), event.getLocation());
            }
            case STEP_HIT -> {
                if (responseProcessor.isStepOverRequestActive()) {
                    int requestId = responseProcessor.getStepOverRequestId();
                    responseProcessor.resetStepOverRequestId();
                    requestClearEvent(EVENT_KIND_SINGLE_STEP, requestId);
                }
                logger.info("STEP OVER HIT line #" + event.getLocation().codeIndex());
                processHitLocation(event.getThread(), event.getLocation());
            }
            case CLASS_LOADED -> {
                currentState.setThreadId(event.getThread());
                currentState.setClassId(event.getRefTypeId());
                requestMethodsOfClassInfo(event.getRefTypeId());
//                classIsLoaded(event.getRefTypeId());
            }
        }
    }

    private static void requestClearEvent(byte eventKind, int requestId) {
        try {
            int id = getNewUniqueId();
            responseProcessor.requestIsSent(id, RESPONSE_TYPE_NONE);
            Packet packet = new Packet(id, EMPTY_FLAGS, EVENT_REQUEST_COMMAND_SET, CLEAR_CMD);
            packet.addDataAsByte(eventKind);
            packet.addDataAsInt(requestId);
            out.write(packet.getPacketBytes());
            out.flush();
        } catch (Exception e) {
            logger.error("something went wrong during requesting clear event");
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

//    private static void idSizes() throws Exception {
//        sendEmptyPacket(getNewUniqueId(),
//                VIRTUAL_MACHINE_COMMAND_SET,
//                ID_SIZES_CMD,
//                RESPONSE_TYPE_ID_SIZES);
//    }

    private static void allClasses() throws Exception {
        sendEmptyPacket(getNewUniqueId(),
        VIRTUAL_MACHINE_COMMAND_SET,
        ALL_CLASSES_CMD,
        RESPONSE_TYPE_ALL_CLASSES);
    }

//    private static void capabilitiesNew() throws Exception {
//        sendEmptyPacket(getNewUniqueId(),
//        VIRTUAL_MACHINE_COMMAND_SET,
//        CAPABILITIES_NEW_CMD,
//        RESPONSE_TYPE_NONE);
//    }

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
            int id = getNewUniqueId();
            Packet packet = new Packet(id, EMPTY_FLAGS, STRING_REFERENCE_COMMAND_SET, STRING_VALUE_CMD);
            packet.addDataAsLong(stringId);
            responseProcessor.requestIsSent(id, RESPONSE_TYPE_STRING_VALUE);
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
            String response = new String(result, StandardCharsets.US_ASCII);
            logger.info(response);
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
        long methodId = currentState.getMethods().stream()
                .filter(method -> method.name().equals(methodName))
                .findAny().map(AMethod::methodId).orElse(0L);
        long classId = currentState.getClassId();
        requestLineTableInfo(classId, methodId); // todo put this somewhere else
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

    private static void requestStepOverEvents() {
        try {
            int id = getNewUniqueId();
            responseProcessor.requestIsSent(id, RESPONSE_TYPE_SINGLE_STEP);
            Packet packet = new Packet(id, EMPTY_FLAGS, EVENT_REQUEST_COMMAND_SET, SET_CMD);
            packet.addDataAsByte(EVENT_KIND_SINGLE_STEP);
            packet.addDataAsByte(SuspendPolicy.ALL);
            packet.addDataAsInt(1); // modifiers
            packet.addDataAsByte(ModKind.STEP);
            packet.addDataAsLong(currentState.getThreadId());
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

    private static void requestExit() {
        try {
            int exitCode = EXIT_CODE_OK;
            Packet packet = new Packet(id, EMPTY_FLAGS, VIRTUAL_MACHINE_COMMAND_SET, EXIT_CMD);
            packet.addDataAsInt(exitCode);
            out.write(packet.getPacketBytes());
            out.flush();
            System.exit(exitCode);
        } catch (Exception e) {
            logger.error("something went wrong during exiting");
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

    private static void finishAndStop() {
        responseProcessor.finishProcessing();
        userInputProcessor.finishProcessing();
        stopConnection();
        keepProcessing = false;
    }

    private static void methodsInfoObtained(List<AMethod> methodList) {
        currentState.setMethods(methodList);
        if (!userCanInteract) { // we are here first time when the first class is being loaded. do rewrite this bit
            userCanInteract = true;
            logger.info("class is ready to be debugged. please enter command");
        }
    }

    private static void logCurrentMethod() {
        currentState.getMethods().forEach(method ->
                currentState.getLocation().ifPresent(l -> {
                    if (l.methodId() == method.methodId()) {
                        logger.info("current method: " + method.name());
                    }
                }));
    }

    private static void processHitLocation(long threadId, Location location) {
        if (currentState.getClassId() != location.classId() ||
                currentState.getLocation().filter(l ->
                        l.methodId() == location.methodId()).isEmpty()) {
            long classId = location.classId();
            long methodId = location.methodId();
            if (currentState.getClassId() != classId) {
                requestMethodsOfClassInfo(classId);
            }
            requestByteCodes(classId, methodId);
            requestVariableTableInfo(classId, methodId);
        }
        currentState.setLocation(location);
        requestCurrentFrameInfo(threadId);
    }

    private static void logCurrentOperation() {
        currentState.getBytecodes().ifPresent(byteCodes ->
                currentState.getLocation().map(Location::codeIndex).ifPresent(codeIndex ->
                        logger.info("current operation: " +
                                (codeIndex < byteCodes.length ?
                                        BYTECODE_OPERATIONS.get(byteCodes[codeIndex.intValue()]) :
                                        "unknown"))));
    }

    private static void requestLocalVariables(long frameId) {
        currentState.getLocation().map(Location::codeIndex).flatMap(codeIndex ->
            currentState.getVariableTable().map(variableTable ->
                variableTable.getVariables().stream()
                        .filter(variable -> codeIndex > variable.codeIndex() &&
                                codeIndex < variable.codeIndex() + variable.length()).toList()
            )
        ).ifPresent(visibleVariables -> {
                    if (visibleVariables.size() > 0) {
                        int id = getNewUniqueId();
                        responseProcessor.requestIsSent(id, RESPONSE_TYPE_LOCAL_VARIABLES);
                        Packet packet = new Packet(id, EMPTY_FLAGS, STACK_FRAME_COMMAND_SET, GET_VALUES);
                        packet.addDataAsLong(currentState.getThreadId());
                        packet.addDataAsLong(frameId);
                        packet.addDataAsInt(visibleVariables.size()); // amount of variables
                        for (Variable variable : visibleVariables) {
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
                }
        );
    }

    private static void logLocalVariables(List<GenericVariable> variables) {
        logger.info("LOCAL VARIABLES:");
        for (GenericVariable variable: variables) {
            switch (variable.type()) {
                case Type.INT -> logger.info("int, value: " + variable.value());
                case Type.ARRAY -> logger.info("array, reference: " + variable.value());
                case Type.STRING -> requestStringValue(variable.value());
                case Type.OBJECT -> logger.info("object, reference: " + variable.value());
                default -> logger.error("something unexpected received");
            }
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
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
        requestResume();
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
            }
        }
    }

    private static void requestClearEvent(byte eventKind, int requestId) {
        Packet packet = new Packet(getNewUniqueId(), EMPTY_FLAGS, EVENT_REQUEST_COMMAND_SET, CLEAR_CMD);
        packet.addDataAsByte(eventKind);
        packet.addDataAsInt(requestId);
        sendPacket(packet, RESPONSE_TYPE_NONE);
    }

    private static void requestByteCodes(long classId, long methodId) {
        Packet packet = new Packet(getNewUniqueId(), EMPTY_FLAGS, METHOD_COMMAND_SET, BYTECODES_CMD);
        packet.addDataAsLong(classId);
        packet.addDataAsLong(methodId);
        sendPacket(packet, RESPONSE_TYPE_BYTECODES);
    }

    private static void requestAllClasses() {
        sendPacket(
                new Packet(
                        getNewUniqueId(),
                        EMPTY_FLAGS,
                        VIRTUAL_MACHINE_COMMAND_SET,
                        ALL_CLASSES_CMD
                ), RESPONSE_TYPE_ALL_CLASSES);
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
        int id = getNewUniqueId();
        Packet packet = new Packet(id, EMPTY_FLAGS, STRING_REFERENCE_COMMAND_SET, STRING_VALUE_CMD);
        packet.addDataAsLong(stringId);
        sendPacket(packet, RESPONSE_TYPE_STRING_VALUE);
    }

    private static void sendPacket(Packet packet, int responseType) {
        if (responseType != RESPONSE_TYPE_NONE) responseProcessor.requestIsSent(packet.getId(), responseType);
        try {
            out.write(packet.getPacketBytes());
            out.flush();
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    // todo test this
    private static void requestClassDataBySignature(String signature) {
        Packet packet = new Packet(getNewUniqueId(), EMPTY_FLAGS, VIRTUAL_MACHINE_COMMAND_SET, CLASSES_BY_SIGNATURE_CMD);
        packet.addDataAsInt(signature.getBytes().length);
        packet.addDataAsBytes(signature.getBytes());
        sendPacket(packet, RESPONSE_TYPE_CLASS_INFO);
    }

    private static void requestClassPrepareEvent(String className) throws Exception {
        Packet packet = new Packet(getNewUniqueId(), EMPTY_FLAGS, EVENT_REQUEST_COMMAND_SET, SET_CMD);
        packet.addDataAsByte(EVENT_KIND_CLASS_PREPARE);
        packet.addDataAsByte(SuspendPolicy.ALL);
        packet.addDataAsInt(1); // modifiers
        packet.addDataAsByte(ModKind.CLASS_MATCH);
        byte[] classNameBytes = className.getBytes();
        packet.addDataAsBytes(Utils.getBytesOfInt(classNameBytes.length)); // todo addDataAsInt?
        packet.addDataAsBytes(classNameBytes);
        sendPacket(packet, RESPONSE_TYPE_COMPOSITE_EVENT);

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
        Packet packet = new Packet(id, EMPTY_FLAGS, EVENT_REQUEST_COMMAND_SET, SET_CMD);
        packet.addDataAsByte(EVENT_KIND_BREAKPOINT);
        packet.addDataAsByte(SuspendPolicy.ALL);
        packet.addDataAsInt(1); // modifiers
        packet.addDataAsByte(ModKind.LOCATION_ONLY);
        packet.addDataAsByte(TypeTag.CLASS);
        packet.addDataAsLong(classId);
        packet.addDataAsLong(methodId);
        packet.addDataAsLong(codeIndex);
        sendPacket(packet, RESPONSE_TYPE_COMPOSITE_EVENT);
    }

    private static void requestStepOverEvents() {
        Packet packet = new Packet(getNewUniqueId(), EMPTY_FLAGS, EVENT_REQUEST_COMMAND_SET, SET_CMD);
        packet.addDataAsByte(EVENT_KIND_SINGLE_STEP);
        packet.addDataAsByte(SuspendPolicy.ALL);
        packet.addDataAsInt(1); // modifiers
        packet.addDataAsByte(ModKind.STEP);
        packet.addDataAsLong(currentState.getThreadId());
        packet.addDataAsInt(Step.STEP_MIN);
        packet.addDataAsInt(Step.STEP_OVER);
        sendPacket(packet, RESPONSE_TYPE_SINGLE_STEP);
    }

    private static void requestMethodsOfClassInfo(long typeID) {
        Packet packet = new Packet(getNewUniqueId(), EMPTY_FLAGS, REFERENCE_TYPE_COMMAND_SET, METHODS_CMD);
        packet.addDataAsLong(typeID);
        sendPacket(packet, RESPONSE_TYPE_METHODS);
    }

    private static void requestCurrentFrameInfo(long threadId) {
        Packet packet = new Packet(getNewUniqueId(), EMPTY_FLAGS, THREAD_REFERENCE_COMMAND_SET, FRAMES);
        packet.addDataAsLong(threadId);
        packet.addDataAsInt(0); // current frame
        packet.addDataAsInt(1); // amount of frames to retrieve
        sendPacket(packet, RESPONSE_TYPE_FRAME_INFO);
    }

    /*
    Either line table or variable table
     */
    private static void requestTableInfo(long classId, long methodId, int cmd) {
        Packet packet = new Packet(getNewUniqueId(), EMPTY_FLAGS, METHOD_COMMAND_SET, cmd);
        packet.addDataAsLong(classId);
        packet.addDataAsLong(methodId);
        sendPacket(packet, cmd == LINETABLE_CMD ? RESPONSE_TYPE_LINETABLE : RESPONSE_TYPE_VARIABLETABLE);
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
        sendPacket(new Packet(getNewUniqueId(), EMPTY_FLAGS, VIRTUAL_MACHINE_COMMAND_SET, RESUME_CMD), RESPONSE_TYPE_NONE);
    }

    private static void requestExit() {
        int exitCode = EXIT_CODE_OK;
        Packet packet = new Packet(id, EMPTY_FLAGS, VIRTUAL_MACHINE_COMMAND_SET, EXIT_CMD);
        packet.addDataAsInt(exitCode);
        sendPacket(packet, EXIT_CMD);
        System.exit(exitCode);
    }

    private static void userRequestClearBreakpoints() {
        sendPacket(
                new Packet(
                        getNewUniqueId(),
                        EMPTY_FLAGS,
                        EVENT_REQUEST_COMMAND_SET,
                        CLEAR_ALL_BREAKPOINTS_CMD
                ), RESPONSE_TYPE_NONE);
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
                        Packet packet = new Packet(getNewUniqueId(), EMPTY_FLAGS, STACK_FRAME_COMMAND_SET, GET_VALUES);
                        packet.addDataAsLong(currentState.getThreadId());
                        packet.addDataAsLong(frameId);
                        packet.addDataAsInt(visibleVariables.size()); // amount of variables
                        for (Variable variable : visibleVariables) {
                            packet.addDataAsInt(variable.slot()); // index in locals array
                            packet.addDataAsByte(Type.getTypeBySignature(variable.signature()));
                        }
                        sendPacket(packet, RESPONSE_TYPE_LOCAL_VARIABLES);
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
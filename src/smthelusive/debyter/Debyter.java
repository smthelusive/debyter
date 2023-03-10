package smthelusive.debyter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smthelusive.debyter.constants.*;
import smthelusive.debyter.domain.*;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


import static smthelusive.debyter.constants.Command.*;
import static smthelusive.debyter.constants.CommandSet.*;
import static smthelusive.debyter.constants.EventKind.*;
import static smthelusive.debyter.constants.Constants.*;
import static smthelusive.debyter.constants.ResponseType.*;

public class Debyter implements ResponseListener, UserInputListener {

    private static Socket clientSocket;
    private static OutputStream out;
    private static InputStream in;
    private static final BlockingQueue<ResponsePacket> responsePackets = new LinkedBlockingQueue<>();
    private static final BlockingQueue<UserCommand> userCommands = new LinkedBlockingQueue<>();
    private static final CurrentState currentState = new CurrentState();
    private static ResponseProcessor responseProcessor;
    private static UserInputProcessor userInputProcessor;
    private static boolean keepProcessing = true;
    private static boolean sameEventBatch = false;
    private static final Logger logger = LoggerFactory.getLogger(Debyter.class);
    private static int id = 0;
    private static final HashSet<DeferredBreakpoint> deferredBreakpoints = new HashSet<>();

    private static int getNewUniqueId() {
        return id++;
    }

    private static final HashMap<Integer, Long> listMethodsRequestsForClass = new HashMap<>();

    private static void createDeferredBreakpoint(String className, String methodName, long codeIndex) {
        deferredBreakpoints.add(new DeferredBreakpoint(className, methodName, codeIndex));
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            logger.info("no address specified, connecting to default address: {}:{}", LOCALHOST, PORT);
            startConnection(LOCALHOST, PORT);
        } else {
            String[] address = args[0].split(":");
            if (address.length != 2)
                throw new Exception("invalid address, please specify address in format {host}:{port}");
            startConnection(address[0], Integer.parseInt(address[1]));
        }
        jdwpHandshake();
        Debyter debyter = new Debyter();
        userInputProcessor = new UserInputProcessor();
        userInputProcessor.addListener(debyter);
        userInputProcessor.start();
        responseProcessor = new ResponseProcessor(in);
        responseProcessor.addListener(debyter);
        responseProcessor.start();
        requestAllThreads();
        requestAllClasses();
        while (keepProcessing) {
            processAllResponsePackets();
            processAUserCommand();
        }
    }

    private static void processAUserCommand() {
        if (!userCommands.isEmpty()) {
            UserCommand userCommand = userCommands.poll();
            switch (userCommand.userCommandType()) {
                case BREAKPOINT -> userRequestBreakpoint(
                        userCommand.params()[0],
                        userCommand.params()[1],
                        Integer.parseInt(userCommand.params()[2]));
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
                case RESPONSE_TYPE_COMPOSITE_EVENT -> {
                    sameEventBatch = false;
                    responsePacket.getEventsList().forEach(Debyter::processEvent);
                }
                case RESPONSE_TYPE_FRAME_INFO -> {
                    logCurrentLocation();
                    currentState.setFrameId(responsePacket.getFrameId());
                    requestLocalVariables(currentState.getFrameId());
                }
                case RESPONSE_TYPE_BYTECODES -> currentState.setBytecodes(responsePacket.getBytecodes());
                case RESPONSE_TYPE_VARIABLETABLE -> currentState.setVariableTable(responsePacket.getVariableTable());
                case RESPONSE_TYPE_LINETABLE -> logger.info("Linetable received: " + responsePacket.getLineTable());
                case RESPONSE_TYPE_LOCAL_VARIABLES -> logLocalVariables(responsePacket.getGenericVariables());
                case RESPONSE_TYPE_CLASS_INFO -> {}
                case RESPONSE_TYPE_ALL_CLASSES -> currentState.setClasses(responsePacket.getClasses());
                case RESPONSE_TYPE_EVENT_REQUEST -> {}
                case RESPONSE_TYPE_METHODS -> {
                    if (Optional.ofNullable(responsePacket.getMethods()).isPresent()) {
                        methodsInfoObtained(responsePacket.getId(), responsePacket.getMethods());
                    }
                }
                case RESPONSE_TYPE_ALL_THREADS ->
                        currentState.setActiveThreads(responsePacket.getActiveThreads());
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
                cleanupAllActiveStepOverRequests();
                logger.info("STEP OVER HIT line #" + event.getLocation().codeIndex());
                processHitLocation(event.getThread(), event.getLocation());
            }
            case CLASS_LOADED -> {
                logger.info("class loaded: " + event.getSignature());
                currentState.setThreadId(event.getThread());
                requestAllClasses();
                requestMethodsOfClassInfo(event.getRefTypeId());
            }
        }
    }

    /***
     * if JVM reported before that step event was registered and this event has been now received,
     * all step events should be cancelled (it requires a separate packet to be thrown to JVM)
     * otherwise stepping will continue forever.
     * there might be more than one request for step event,
     * because sometimes no information on the thread is present, so it's requested for every active thread.
     */
    private static void cleanupAllActiveStepOverRequests() {
        if (!responseProcessor.getStepOverRegisteredRequests().isEmpty()) {
            // saving to a separate list because in the meantime some more requests might get registered todo check if that's still the case
            ArrayList<Integer> requestsToClean = new ArrayList<>(responseProcessor.getStepOverRegisteredRequests());
            responseProcessor.resetStepOverRequests();
            for (Integer requestId : requestsToClean) {
                requestClearEvent(EVENT_KIND_SINGLE_STEP, requestId);
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

    private static void requestAllThreads() {
        sendPacket(
                new Packet(
                        getNewUniqueId(),
                        EMPTY_FLAGS,
                        VIRTUAL_MACHINE_COMMAND_SET,
                        ALL_THREADS_CMD
                ), RESPONSE_TYPE_ALL_THREADS);
    }

    private static void requestStringValue(long stringId) {
        int id = getNewUniqueId();
        Packet packet = new Packet(id, EMPTY_FLAGS, STRING_REFERENCE_COMMAND_SET, STRING_VALUE_CMD);
        packet.addDataAsLong(stringId);
        sendPacket(packet, RESPONSE_TYPE_STRING_VALUE);
    }

    private static void sendPacket(Packet packet, int responseType) {
        if (responseType != RESPONSE_TYPE_NONE)
            responseProcessor.requestIsSent(packet.getId(), responseType);
        try {
            out.write(packet.getPacketBytes());
            out.flush();
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    private static void requestClassPrepareEvent(String className) {
        Packet packet = new Packet(getNewUniqueId(), EMPTY_FLAGS, EVENT_REQUEST_COMMAND_SET, SET_CMD);
        packet.addDataAsByte(EVENT_KIND_CLASS_PREPARE);
        packet.addDataAsByte(SuspendPolicy.ALL);
        packet.addDataAsInt(1); // modifiers
        packet.addDataAsByte(ModKind.CLASS_MATCH);
        byte[] classNameBytes = className.getBytes();
        packet.addDataAsInt(classNameBytes.length);
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

    private static void userRequestBreakpoint(String className, String methodName, long codeIndex) {
        try {
            if (currentState.classMethodInfoAvailable(className, methodName)) {
                long classId = currentState.getClassIdByName(className);
                logger.info("requesting breakpoint right away");
                requestBreakpointEvent(classId, currentState.getMethodIdByClassAndName(classId, methodName), codeIndex);
            } else if (currentState.classInfoAvailable(className)) {
                logger.info("method info is not yet loaded, setting the deferred breakpoint");
                requestSuspend();
                requestMethodsOfClassInfo(currentState.getClassIdByName(className));
                createDeferredBreakpoint(className, methodName, codeIndex);
            } else {
                logger.info("class info is not available yet, setting the deferred breakpoint");
                requestClassPrepareEvent(className);
                createDeferredBreakpoint(className, methodName, codeIndex);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        if (currentState.getThreadId() != 0) {
            requestStepOverForThread(currentState.getThreadId());
        } else {
            for (Long threadId : currentState.getActiveThreads()) {
                requestStepOverForThread(threadId);
            }
        }
    }

    private static void requestStepOverForThread(long threadId) {
        int requestId = getNewUniqueId();
        Packet packet = new Packet(requestId, EMPTY_FLAGS, EVENT_REQUEST_COMMAND_SET, SET_CMD);
        packet.addDataAsByte(EVENT_KIND_SINGLE_STEP);
        packet.addDataAsByte(SuspendPolicy.ALL);
        packet.addDataAsInt(1); // modifiers
        packet.addDataAsByte(ModKind.STEP);
        packet.addDataAsLong(threadId);
        packet.addDataAsInt(Step.STEP_MIN);
        packet.addDataAsInt(Step.STEP_OVER);
        sendPacket(packet, RESPONSE_TYPE_COMPOSITE_EVENT);
        // registering it so later it can be cancelled:
        responseProcessor.addStepOverRequest(requestId);
    }

    private static void requestMethodsOfClassInfo(long typeID) {
        int id = getNewUniqueId();
        listMethodsRequestsForClass.put(id, typeID);
        Packet packet = new Packet(id, EMPTY_FLAGS, REFERENCE_TYPE_COMMAND_SET, METHODS_CMD);
        packet.addDataAsLong(typeID);
        sendPacket(packet, RESPONSE_TYPE_METHODS);
    }

    private static void requestCurrentFrameInfo(long threadId) {
        Packet packet = new Packet(getNewUniqueId(), EMPTY_FLAGS, THREAD_REFERENCE_COMMAND_SET, FRAMES);
        packet.addDataAsLong(threadId);
        packet.addDataAsInt(0); // current frame
        packet.addDataAsInt(1); // amount of frames to retrieve, request the top frame only
        sendPacket(packet, RESPONSE_TYPE_FRAME_INFO);
    }

    /*
        either line table or variable table
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

    private static void requestSuspend() {
        sendPacket(new Packet(getNewUniqueId(), EMPTY_FLAGS, VIRTUAL_MACHINE_COMMAND_SET, SUSPEND_CMD), RESPONSE_TYPE_NONE);
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

    private static void methodsInfoObtained(int id, List<AMethod> methodList) {
        long classId = listMethodsRequestsForClass.get(id);
        for (AMethod method : methodList) {
            currentState.addMethodForClassId(classId, method);
        }
        listMethodsRequestsForClass.remove(id);
        currentState.getClasses().stream()
                .filter(aClass -> aClass.typeID() == classId)
                        .map(AClass::signature).forEach(className ->
                    currentState.getMethodsOfClassId(classId).ifPresent(methodSet -> methodSet.forEach(method ->
                        deferredBreakpoints.forEach(breakpoint -> {
                            if (Utils.getInternalRepresentationOfObjectType(breakpoint.className()).equals(className) &&
                                    breakpoint.methodName().equals(method.name())) {
                                logger.info("requesting breakpoint that was deferred: {}", breakpoint);
                                deferredBreakpoints.remove(breakpoint);
                                requestBreakpointEvent(classId, method.methodId(), breakpoint.codeIndex());
                            }
                        }))));
        /*
         JVM has been suspended by suspend policy = ALL on receiving CLASS_LOADED event.
         Suspend was necessary because it's only possible to request frame info when JVM is suspended.
         Now, since all the info necessary for this class & methods to set breakpoints is available,
         the resume command can be requested.
         If JVM was suspended somewhere else too, this resume will only decrement
         the amount of resume commands that need to be requested for JVM to continue running.
         */
        requestResume();
    }

    private static void logCurrentLocation() {
        logCurrentClass();
        logCurrentMethod();
        logCurrentOperation();
    }

    private static void logCurrentMethod() {
        currentState.getLocation().stream().flatMap(location ->
                currentState.getMethodsOfClassId(location.classId()).stream().flatMap(Collection::stream)
                        .filter(method -> method.methodId() == location.methodId()))
                                .forEach(method -> logger.info("current method: " + method.name()));
    }

    private static void logCurrentClass() {
        currentState.getLocation().ifPresent(location ->
                        currentState.getClasses().stream().filter(aClass -> aClass.typeID() == location.classId())
                                .forEach(aClass -> logger.info("current class: " + aClass.signature())));
    }

    private static void processHitLocation(long threadId, Location location) {
        if (currentState.isNewLocationClassOrMethod(location)) {
            long classId = location.classId();
            long methodId = location.methodId();
            if ((currentState.getLocation().isEmpty() ||
                    currentState.getLocation().get().classId() != classId) &&
                    currentState.getMethodsOfClassId(classId).isEmpty()) {
                requestMethodsOfClassInfo(classId);
            }
            requestByteCodes(classId, methodId);
            requestVariableTableInfo(classId, methodId);
        }
        currentState.setLocation(location); // can be different code index
        currentState.setThreadId(threadId);
        if (currentState.getFrameId() == -1 ||
                (!sameEventBatch || currentState.getLocation().stream()
                        .noneMatch(currentLocation -> currentLocation.equals(location)))) {
            sameEventBatch = true; // to avoid logging hit info twice
            requestCurrentFrameInfo(threadId); // this flow will proceed to request/log all hit info
        }
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
                case Type.BOOLEAN -> logger.info("boolean: " + variable.value());
                case Type.BYTE -> logger.info("byte: " + variable.value());
                case Type.CHAR -> logger.info("char: " + variable.value());
                case Type.FLOAT -> logger.info("float: " + variable.value());
                case Type.DOUBLE -> logger.info("double: " + variable.value());
                case Type.SHORT -> logger.info("short: " + variable.value());
                case Type.THREAD -> logger.info("thread: " + variable.value());
                case Type.THREAD_GROUP -> logger.info("thread group: " + variable.value());
                case Type.CLASS_LOADER -> logger.info("class loader: " + variable.value());
                case Type.CLASS_OBJECT -> logger.info("class object: " + variable.value());
                case Type.VOID -> logger.info("void: " + variable.value());
                case Type.LONG -> logger.info("long: " + variable.value());
                default -> logger.error("something unexpected received: " + variable.value());
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
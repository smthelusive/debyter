package smthelusive.debyter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smthelusive.debyter.constants.Type;
import smthelusive.debyter.domain.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static smthelusive.debyter.Utils.byteArrayToInteger;
import static smthelusive.debyter.constants.Command.COMPOSITE_EVENT_CMD;
import static smthelusive.debyter.constants.CommandSet.EVENT_COMMAND_SET;
import static smthelusive.debyter.constants.EventKind.*;
import static smthelusive.debyter.constants.Constants.*;
import static smthelusive.debyter.constants.ResponseType.*;
import static smthelusive.debyter.domain.EventType.CLASS_LOADED;
import static smthelusive.debyter.domain.EventType.VM_DEATH;

public class ResponseProcessor extends Thread {
    private final InputStream inputStream;
    private boolean processingOn = true;
    private final Map<Integer, Integer> requestsSent = new HashMap<>();
    private final Logger logger = LoggerFactory.getLogger(ResponseProcessor.class);
    private int lastPos;
    private long classId;
    private long threadId;
    private final ResponseListener responseListener;

    public void requestIsSent(int id, int expectedResponseType) {
        requestsSent.put(id, expectedResponseType);
    }

    public void finishProcessing() {
        this.processingOn = false;
    }
    public ResponseProcessor(InputStream in, ResponseListener responseListener) {
        this.inputStream = in;
        this.responseListener = responseListener;
    }

    /***
     * parse input stream non-stop
     * notify listeners when something is received
     */
    @Override
    public void run() {
        while (processingOn) {
            try {
                byte[] intVal = inputStream.readNBytes(INTEGER_LENGTH_BYTES);
                if (intVal.length < INTEGER_LENGTH_BYTES) continue; // don't know how but this happens
                int replyLength = byteArrayToInteger(intVal);
                int leftoverLengthToRead = replyLength - INTEGER_LENGTH_BYTES;
                if (leftoverLengthToRead > 0) {
                    byte[] result = inputStream.readNBytes(leftoverLengthToRead);
                    ResponsePacket responsePacket = parseResponseIntoPacket(result);
                    responseListener.incomingPacket(responsePacket);
                    requestsSent.remove(responsePacket.getId());
                }
            } catch (IOException ioException) {
                logger.error("exception while accepting the packet: " + ioException);
            }
        }
    }

    private ResponsePacket parseResponseIntoPacket(byte[] result) {
        lastPos = 0;
        ResponsePacket responsePacket = new ResponsePacket();
        int length = result.length + INTEGER_LENGTH_BYTES;
        int idValue = getIntFromData(result);
        byte flag = result[lastPos];
        lastPos++;
        int type;
        short errorCode = 0;
        if (result[lastPos] == EVENT_COMMAND_SET &&
                result[lastPos + 1] == COMPOSITE_EVENT_CMD) { // composite event received
            type = RESPONSE_TYPE_COMPOSITE_EVENT;
            lastPos += 2;
        } else {
            errorCode = getShortFromData(result);
            Integer storedResponseType = Optional.ofNullable(requestsSent.get(idValue)).orElse(0);
            type = storedResponseType.equals(RESPONSE_TYPE_COMPOSITE_EVENT) ?
                    RESPONSE_TYPE_EVENT_REQUEST : storedResponseType;
        }
        if (errorCode == 0) {
            switch (type) {
                case RESPONSE_TYPE_CLASS_INFO -> responsePacket = parseResponseClassInfo(result);
                case RESPONSE_TYPE_ALL_CLASSES -> responsePacket = parseResponseAllClasses(result);
                case RESPONSE_TYPE_ID_SIZES -> responsePacket = parseResponseIdSizes(result);
                case RESPONSE_TYPE_COMPOSITE_EVENT -> responsePacket = parseCompositeEvent(result);
                case RESPONSE_TYPE_EVENT_REQUEST -> {
                    int requestId = getIntFromData(result);
                    requestIsSent(idValue, requestId);
                    responsePacket.setId(requestId);
                }
                case RESPONSE_TYPE_LINETABLE -> responsePacket = parseResponseLinetable(result);
                case RESPONSE_TYPE_METHODS -> responsePacket = parseResponseMethods(result);
                case RESPONSE_TYPE_VARIABLETABLE -> responsePacket = parseResponseVariableTable(result);
                case RESPONSE_TYPE_FRAME_INFO -> responsePacket = parseResponseFrame(result);
                case RESPONSE_TYPE_LOCAL_VARIABLES -> responsePacket = parseResponseVariables(result);
                case RESPONSE_TYPE_BYTECODES -> responsePacket = parseResponseBytecodes(result);
                default -> {}
            }
        }
        responsePacket.setResponseType(type);
        responsePacket.setLength(length);
        responsePacket.setId(idValue);
        responsePacket.setFlag(flag);
        responsePacket.setErrorCode(errorCode);
        return responsePacket;
    }

    private ResponsePacket parseResponseBytecodes(byte[] result) {
        ResponsePacket responsePacket = new ResponsePacket();
        int amountOfBytes = getIntFromData(result);
        byte[] bytes = new byte[amountOfBytes];
        for (int i = 0; i < bytes.length; i++) {
            byte value = result[lastPos];
            lastPos++;
            bytes[i] = value;
        }
        responsePacket.setBytecodes(bytes);
        return responsePacket;
    }

    private ResponsePacket parseResponseVariables(byte[] result) {
        ResponsePacket responsePacket = new ResponsePacket();
        int amountOfValues = getIntFromData(result);
        for (int i = 0; i < amountOfValues; i++) {
            byte type = result[lastPos];
            lastPos++;
            switch (type) {
                case Type.INT -> {
                    int value = getIntFromData(result);
                    GenericVariable intValue = new IntVariable(value);
                    responsePacket.addVariableValue(intValue);
                }
                case Type.ARRAY -> {
                    long arrayRef = getLongFromData(result);
                    // todo
                    logger.info("array, slot " + i + " is: " + arrayRef);
                }
                case Type.STRING -> {
                    long stringReference = getLongFromData(result);
                    // todo
                    logger.info("string reference, slot " + i + " is: " + stringReference);
                }
                case Type.OBJECT -> {
                    long objectReference = getLongFromData(result);
                    // todo
                    logger.info("object reference, slot " + i + " is: " + objectReference);
                }
                default -> logger.error("something unexpected received");
            }
        }
        return responsePacket;
    }

    private ResponsePacket parseResponseFrame(byte[] result) {
        ResponsePacket responsePacket = new ResponsePacket();
        long amount = getIntFromData(result);
        if (amount != 1) logger.error("got more than one frame in response");
        long frameId = getLongFromData(result);
        // location:
        byte tag = result[lastPos];
        lastPos++;
        long classId = getLongFromData(result);
        long methodId = getLongFromData(result);
        long codeIndex = getLongFromData(result);
        Location location = new Location(tag, classId, methodId, codeIndex);
        logger.info("frame location: " + location + " id: " + frameId);
        responsePacket.setFrameId(frameId);
        responsePacket.setLocation(location);
        return responsePacket;
    }

    private ResponsePacket parseResponseLinetable(byte[] result) {
        ResponsePacket responsePacket = new ResponsePacket();
        long start = getLongFromData(result);
        long end = getLongFromData(result);
        int lines = getIntFromData(result);

        LineTable lineTable = new LineTable();
        lineTable.setStart(start);
        lineTable.setEnd(end);
        for (int i = 0; i < lines; i++) {
            long lineCodeIndex = getLongFromData(result);
            int lineNumber = getIntFromData(result);
            lineTable.addLine(lineCodeIndex, lineNumber);
        }
        responsePacket.setLineTable(lineTable);
        return responsePacket;
    }

    private ResponsePacket parseResponseMethods(byte[] result) {
        ResponsePacket responsePacket = new ResponsePacket();
        int amountOfMethods = getIntFromData(result);
        for (int i = 0; i < amountOfMethods; i++) {
            long methodID = getLongFromData(result);
            String methodName = getStringFromData(result);
            String methodSignature = getStringFromData(result);
            int modBits = getIntFromData(result);
            responsePacket.addMethod(methodID, methodName, methodSignature, modBits);
        }
        return responsePacket;
    }

    private ResponsePacket parseResponseAllClasses(byte[] result) {
        ResponsePacket responsePacket = new ResponsePacket();
        int amountOfClasses = getIntFromData(result);
        for (int i = 0; i < amountOfClasses; i++) {
            byte refTypeTag = result[lastPos];
            lastPos++;
            long typeID = getLongFromData(result);
            String signature = getStringFromData(result);
            int status = getIntFromData(result);
            responsePacket.addClass(refTypeTag, typeID, signature, status);
        }
        return responsePacket;
    }

    private ResponsePacket parseResponseClassInfo(byte[] result) {
        ResponsePacket responsePacket = new ResponsePacket();
        int amountOfMatchingClasses = getIntFromData(result);
        for (int i = 0; i < amountOfMatchingClasses; i++) {
            byte refTypeTag = result[lastPos];
            lastPos++;
            long typeID = getLongFromData(result);
            int status = getIntFromData(result);
            responsePacket.addClass(refTypeTag, typeID, status);
        }
        return responsePacket;
    }

    private ResponsePacket parseResponseIdSizes(byte[] result) {
        ResponsePacket responsePacket = new ResponsePacket();
        int fieldIDSize = getIntFromData(result);
        int methodIDSize = getIntFromData(result);
        int objectIDSize = getIntFromData(result);
        int referenceTypeIDSize = getIntFromData(result);
        int frameIDSize = getIntFromData(result);
        responsePacket.setFieldIDSize(fieldIDSize);
        responsePacket.setMethodIDSize(methodIDSize);
        responsePacket.setObjectIDSize(objectIDSize);
        responsePacket.setReferenceTypeIDSize(referenceTypeIDSize);
        responsePacket.setFrameIDSize(frameIDSize);
        logger.info("id sizes: \n objectIdSize: " + objectIDSize);
        return responsePacket;
    }

    private ResponsePacket parseResponseVariableTable(byte[] result) {
        ResponsePacket responsePacket = new ResponsePacket();
        int argCnt = getIntFromData(result);
        int slots = getIntFromData(result);
        VariableTable variableTable = new VariableTable();
        variableTable.setArgCnt(argCnt);
        variableTable.setSlots(slots);
        for (int i = 0; i < slots; i++) {
            long codeIndex = getLongFromData(result);
            String name = getStringFromData(result);
            String signature = getStringFromData(result);
            int length = getIntFromData(result);
            int slot = getIntFromData(result);
            variableTable.addVariable(codeIndex, name, signature, length, slot);
        }
        responsePacket.setVariableTable(variableTable);
        return responsePacket;
    }

    private ResponsePacket parseCompositeEvent(byte[] result) {
        ResponsePacket responsePacket = new ResponsePacket();
        byte suspendPolicy = result[lastPos];
        lastPos++;
        responsePacket.setSuspendPolicy(suspendPolicy);
        int eventsSize = getIntFromData(result);
        responsePacket.setEvents(eventsSize);
        for (int i = 0; i < eventsSize; i++) {
            byte eventKind = result[lastPos];
            lastPos++;
            switch (eventKind) {
                case EVENT_KIND_VM_DEATH -> responsePacket.addEvent(constructVMDeathEvent(result));
                case EVENT_KIND_CLASS_PREPARE -> responsePacket.addEvent(parseClassPrepareEvent(result));
                case EVENT_KIND_BREAKPOINT ->
                        responsePacket.addEvent(parseBreakpointEvent(result, EVENT_KIND_BREAKPOINT));
                case EVENT_KIND_SINGLE_STEP ->
                        responsePacket.addEvent(parseBreakpointEvent(result, EVENT_KIND_SINGLE_STEP));
            }
        }
        return responsePacket;
    }

    private Event constructVMDeathEvent(byte[] result) {
        Event vmDeathEvent = new Event();
        vmDeathEvent.setEventKind(EVENT_KIND_VM_DEATH);
        vmDeathEvent.setInternalEventType(VM_DEATH);
        vmDeathEvent.setRequestID(getIntFromData(result));
        return vmDeathEvent;
    }

    private Event parseClassPrepareEvent(byte[] result) {
        Event event = new Event();
        event.setEventKind(EVENT_KIND_CLASS_PREPARE);
        event.setInternalEventType(CLASS_LOADED);
        event.setRequestID(getIntFromData(result));
        event.setThread(getLongFromData(result));
        byte refTypeTag = result[lastPos];
        lastPos++;
        event.setRefTypeTag(refTypeTag);
        event.setRefTypeId(getLongFromData(result));

        String signature = getStringFromData(result);
        event.setSignature(signature);

        event.setStatus(getIntFromData(result));
        classId = event.getRefTypeId();
        threadId = event.getThread();
        return event;
    }

    private Event parseBreakpointEvent(byte[] result, byte eventKind) {
        Event event = new Event();
        event.setEventKind(eventKind);
        event.setInternalEventType(eventKind == EVENT_KIND_BREAKPOINT ?
                EventType.BREAKPOINT_HIT : EventType.STEP_HIT);
        int requestId = getIntFromData(result);
        event.setRequestID(requestId);
        long threadId = getLongFromData(result);
        event.setThread(threadId);
        byte tag = result[lastPos];
        lastPos++;
        long classId = getLongFromData(result);
        long methodId = getLongFromData(result);
        long codeIndex = getLongFromData(result);
        Location location = new Location(tag, classId, methodId, codeIndex);
        event.setLocation(location);
        return event;
    }

    private int getIntFromData(byte[] result) {
        byte[] valueInBytes = new byte[INTEGER_LENGTH_BYTES];
        System.arraycopy(result, lastPos, valueInBytes, 0, INTEGER_LENGTH_BYTES);
        lastPos += INTEGER_LENGTH_BYTES;
        return Utils.byteArrayToInteger(valueInBytes);
    }

    private short getShortFromData(byte[] result) {
        byte[] valueInBytes = new byte[SHORT_LENGTH_BYTES];
        valueInBytes[0] = result[lastPos];
        valueInBytes[1] = result[lastPos + 1];
        lastPos += SHORT_LENGTH_BYTES;
        return  Utils.byteArrayToShort(valueInBytes);
    }

    private long getLongFromData(byte[] result) {
        byte[] valueInBytes = new byte[LONG_LENGTH_BYTES];
        System.arraycopy(result, lastPos, valueInBytes, 0, LONG_LENGTH_BYTES);
        lastPos += LONG_LENGTH_BYTES;
        return Utils.byteArrayToLong(valueInBytes);
    }

    private String getStringFromData(byte[] result) {
        int stringSize = getIntFromData(result);
        byte[] stringValueInBytes = new byte[stringSize];
        System.arraycopy(result, lastPos, stringValueInBytes, 0, stringSize);
        lastPos+=stringSize;
        return new String(stringValueInBytes, StandardCharsets.US_ASCII);
    }
}
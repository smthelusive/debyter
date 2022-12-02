package smthelusive.debyter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smthelusive.debyter.domain.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static smthelusive.debyter.Utils.byteArrayToInteger;
import static smthelusive.debyter.constants.Command.COMPOSITE_EVENT_CMD;
import static smthelusive.debyter.constants.CommandSet.EVENT_COMMAND_SET;
import static smthelusive.debyter.constants.ResponseType.*;
import static smthelusive.debyter.constants.Constants.*;
import static smthelusive.debyter.domain.EventKind.*;

public class ResponseProcessor extends Thread {
    private final InputStream inputStream;
    private boolean processingOn = true;
    private final Map<Integer, Integer> requestsSent = new HashMap<>();
    private final Logger logger = LoggerFactory.getLogger(ResponseProcessor.class);
    private int lastPos;

    private long classId;
    private final ResponseNotifier notifier;

    public void requestIsSent(int id, int expectedResponseType) {
        requestsSent.put(id, expectedResponseType);
    }

    public void finishProcessing() {
        this.processingOn = false;
    }
    public ResponseProcessor(InputStream in, ResponseNotifier notifier) {
        this.inputStream = in;
        this.notifier = notifier;
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
                int replyLength = byteArrayToInteger(intVal);
                int leftoverLengthToRead = replyLength - INTEGER_LENGTH_BYTES;
                if (leftoverLengthToRead > 0) {
                    byte[] result = inputStream.readNBytes(leftoverLengthToRead);
                    ResponsePacket responsePacket = parseResponseIntoPacket(result);
                    notifier.notifyAboutIncomingPacket(responsePacket);
                    requestsSent.remove(responsePacket.getId());
                }
            } catch (IOException ioException) {
                logger.error("exception while accepting the packet: " + ioException.getMessage());
            }
        }
        notifier.notifyFinishedProcessing();
    }

    private ResponsePacket parseResponseIntoPacket(byte[] result) {
        lastPos = 0;
        logger.info(new String(result));
        ResponsePacket responsePacket = new ResponsePacket(); // todo make a responsepacket builder
        int length = result.length + INTEGER_LENGTH_BYTES;
        int idValue = getIntFromData(result);
        byte flag = result[lastPos];
        lastPos++;
        int type;
        short errorCode = 0;
        if (result[lastPos] == EVENT_COMMAND_SET && result[lastPos + 1] == COMPOSITE_EVENT_CMD) { // composite event received
            type = RESPONSE_TYPE_COMPOSITE_EVENT;
            lastPos += 2;
        } else {
            errorCode = getShortFromData(result);
            if (Optional.ofNullable(requestsSent.get(idValue)).orElse(0)
                    .equals(RESPONSE_TYPE_COMPOSITE_EVENT)) {
                // received response for event request with another id
                type = RESPONSE_TYPE_EVENT_REQUEST;
            } else {
                type = Optional.ofNullable(requestsSent.get(idValue)).orElse(0);
            }
        }
        switch (type) {
            case RESPONSE_TYPE_CLASS_INFO:
                responsePacket = parseResponseClassInfo(result);
                break;
            case RESPONSE_TYPE_ALL_CLASSES:
                responsePacket = parseResponseAllClasses(result);
                break;
            case RESPONSE_TYPE_COMPOSITE_EVENT:
                if (errorCode == 0) responsePacket = parseCompositeEvent(result);
                break;
            case RESPONSE_TYPE_EVENT_REQUEST:
                int requestId = getIntFromData(result);
                requestIsSent(idValue, requestId);
                break;
            case RESPONSE_TYPE_LINETABLE:
                responsePacket = parseResponseLinetable(result);
                break;
            case RESPONSE_TYPE_METHODS:
                responsePacket = parseResponseMethods(result);
                break;
            default: logger.info(Arrays.toString(result));
        }
        responsePacket.setResponseType(type);
        responsePacket.setLength(length);
        responsePacket.setId(idValue);
        responsePacket.setFlag(flag);
        responsePacket.setErrorCode(errorCode);
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
            responsePacket.setLineTable(lineTable);
        }
        notifier.notifyBreakpointInfoObtained(lineTable);
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
        notifier.notifyClassMethodsInfoObtained(classId, responsePacket.getMethods());
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
                case VM_DEATH -> logger.error("Event VM_DEATH was raised");
                case CLASS_PREPARE -> responsePacket.addEvent(parseClassPrepareEvent(result));
                case BREAKPOINT -> responsePacket.addEvent(parseBreakpointEvent(result));
            }
        }
        return responsePacket;
    }

    private Event parseClassPrepareEvent(byte[] result) {
        Event event = new Event();
        event.setEventKind(CLASS_PREPARE);
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
        notifier.notifyClassLoaded(classId);
        return event;
    }

    private Event parseBreakpointEvent(byte[] result) {
        Event event = new Event();
        event.setEventKind(BREAKPOINT);
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
        notifier.notifyBreakpointHit(requestId, threadId, location);
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
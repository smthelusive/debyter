package smthelusive.debyter;

import smthelusive.debyter.domain.Event;
import smthelusive.debyter.domain.ResponsePacket;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static smthelusive.debyter.Utils.byteArrayToInteger;
import static smthelusive.debyter.domain.Constants.*;
import static smthelusive.debyter.domain.EventKind.*;

public class ResponseProcessor extends Thread {
    private final InputStream inputStream;
    private boolean processingOn = true;
    private final Map<Integer, Integer> requestsSent = new HashMap<>();
    private int lastPos = 0;
    private byte[] result;

    public void requestIsSent(int id, int expectedResponseType) {
        requestsSent.put(id, expectedResponseType);
    }

    public void finishProcessing() {
        this.processingOn = false;
    }
    public ResponseProcessor(InputStream in) {
        this.inputStream = in;
    }
    private final List<ResponseListener> listeners = new ArrayList<>();

    public void addListener(ResponseListener listener) {
        listeners.add(listener);
    }

    private void notifyAboutIncomingPacket(ResponsePacket packet) {
        for (ResponseListener listener : listeners)
            listener.incomingPacket(packet);
    }

    private void notifyFinishedProcessing() {
        for (ResponseListener listener : listeners)
            listener.finish();
    }

    /***
     * parse input stream non-stop
     * notify listeners when the full response packet is received
     */
    @Override
    public void run() {
        while (processingOn || !requestsSent.isEmpty()) {
            if (!requestsSent.isEmpty()) {
                try {
                    byte[] intVal = inputStream.readNBytes(4);
                    int replyLength = byteArrayToInteger(intVal);
                    int leftoverLengthToRead = replyLength - (replyLength >= 4 ? 4 : 0);
                    result = inputStream.readNBytes(leftoverLengthToRead);
                    ResponsePacket responsePacket = parseResponseIntoPacket();
                    notifyAboutIncomingPacket(responsePacket);
                    requestsSent.remove(responsePacket.getId());
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            } else notifyFinishedProcessing();
        }
    }

    private ResponsePacket parseResponseIntoPacket() {
        System.out.println(new String(result));
        ResponsePacket responsePacket = new ResponsePacket();
        responsePacket.setLength(result.length + 4);

        int idValue = getIntFromData();
        responsePacket.setId(idValue);
        responsePacket.setFlag(result[lastPos]);
        lastPos++;
        int type;
        if (result[lastPos] == 64 && result[lastPos + 1] == 100) { // composite event received
            type = RESPONSE_TYPE_COMPOSITE_EVENT;
        } else {
            responsePacket.setErrorCode(getShortFromData());
            type = Optional.ofNullable(requestsSent.get(idValue)).orElse(0);
        }

        responsePacket.setResponseType(type);
        int lastPos = 7;
        switch (type) {
            case RESPONSE_TYPE_CLASS_INFO:
                int amountOfClasses = getIntFromData();
                for (int i = 0; i < amountOfClasses; i++) {
                    byte refTypeTag = result[lastPos];
                    lastPos++;
                    long typeID = getLongFromData();
                    int status = getIntFromData();
                    responsePacket.addClass(refTypeTag, typeID, status);
                }
                break;
            case RESPONSE_TYPE_ALL_CLASSES:
                amountOfClasses = getIntFromData();
                for (int i = 0; i < amountOfClasses; i++) {
                    byte refTypeTag = result[lastPos];
                    lastPos++;
                    long typeID = getLongFromData();
                    int stringSize = getIntFromData();
                    byte[] stringValueInBytes = new byte[stringSize];
                    System.arraycopy(result, lastPos, stringValueInBytes, 0, stringSize);
                    String signature = new String(stringValueInBytes, StandardCharsets.US_ASCII);
                    lastPos+=stringSize;
                    int status = getIntFromData();
                    responsePacket.addClass(refTypeTag, typeID, signature, status);
                }
                break;
            case RESPONSE_TYPE_COMPOSITE_EVENT:
                enrichCompositeEvent(responsePacket);
        }
        return responsePacket;
    }

    private void enrichCompositeEvent(ResponsePacket responsePacket) {
        byte suspendPolicy = result[lastPos];
        lastPos++;
        responsePacket.setSuspendPolicy(suspendPolicy);

        int eventsSize = getIntFromData();
        responsePacket.setEvents(eventsSize);
        for (int i = 0; i < eventsSize; i++) {
            Event event = new Event();
            byte eventKind = result[lastPos];
            lastPos++;
            event.setEventKind(eventKind);
            switch (eventKind) {
                case VM_DEATH:
                    System.out.println("Event VM_DEATH was raised");
                    break;
                case CLASS_PREPARE:
                    System.out.println("class prepare event");
                    event.setRequestID(getIntFromData());
                    event.setThread(getLongFromData());
                    byte refTypeTag = result[lastPos];
                    lastPos++;
                    event.setRefTypeTag(refTypeTag);
                    event.setRefTypeId(getLongFromData());
                    int stringSize = getIntFromData();
                    byte[] stringValueInBytes = new byte[stringSize];
                    System.arraycopy(result, lastPos, stringValueInBytes, 0, stringSize);
                    String signature = new String(stringValueInBytes, StandardCharsets.US_ASCII);
                    lastPos += stringSize;
                    event.setSignature(signature);
                    event.setStatus(getIntFromData());
                    break;
            }

            responsePacket.addEvent(event);
        }
    }

    private int getIntFromData() {
        byte[] valueInBytes = new byte[4];
        System.arraycopy(result, lastPos, valueInBytes, 0, 4);
        lastPos += 4;
        return Utils.byteArrayToInteger(valueInBytes);
    }

    private short getShortFromData() {
        byte[] valueInBytes = new byte[2];
        valueInBytes[0] = result[lastPos];
        valueInBytes[1] = result[lastPos + 1];
        lastPos += 2;
        return  Utils.byteArrayToShort(valueInBytes);
    }

    private long getLongFromData() {
        byte[] valueInBytes = new byte[8];
        System.arraycopy(result, lastPos, valueInBytes, 0, 8);
        lastPos+=8;
        return Utils.byteArrayToLong(valueInBytes);
    }
}
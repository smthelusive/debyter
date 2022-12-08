package smthelusive.debyter;

import smthelusive.debyter.domain.*;

import java.util.ArrayList;
import java.util.List;

public class ResponseNotifier {
    private final List<ResponseListener> listeners = new ArrayList<>();

    public void addListener(ResponseListener listener) {
        listeners.add(listener);
    }

    void notifyAboutIncomingPacket(ResponsePacket packet) {
        for (ResponseListener listener : listeners)
            listener.incomingPacket(packet);
    }

    void notifyFinishedProcessing() {
        for (ResponseListener listener : listeners)
            listener.finish();
    }

    void notifyClassLoaded(long refTypeId) {
        for (ResponseListener listener : listeners)
            listener.classIsLoaded(refTypeId);
    }

    void notifyClassMethodsInfoObtained(long threadId, long classId, List<AMethod> methods) {
        for (ResponseListener listener : listeners)
            listener.classAndMethodsInfoObtained(threadId, classId, methods);
    }

    void notifyVariableTableObtained(VariableTable variableTable) {
        for (ResponseListener listener : listeners)
            listener.variableTableObtained(variableTable);
    }

    void notifyBreakpointInfoObtained(LineTable lineTable) {
        for (ResponseListener listener : listeners)
            listener.lineTableObtained(lineTable);
    }

    void notifyBreakpointHit(long threadId, Location location) {
        for (ResponseListener listener : listeners)
            listener.breakPointHit(threadId, location);
    }

    void notifyFrameInfoObtained(long frameId) {
        for (ResponseListener listener : listeners)
            listener.frameIdObtained(frameId);
    }
    void notifyLocalVariablesObtained() {
        for (ResponseListener listener : listeners)
            listener.variablesReceived();
    }

    void notifyBytecodesObtained(byte[] bytecodes) {
        for (ResponseListener listener : listeners)
            listener.bytecodesReceived(bytecodes);
    }

}

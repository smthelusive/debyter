package smthelusive.debyter;

import smthelusive.debyter.domain.AMethod;
import smthelusive.debyter.domain.LineTable;
import smthelusive.debyter.domain.Location;
import smthelusive.debyter.domain.ResponsePacket;

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

    void notifyBreakpointInfoObtained(LineTable lineTable) {
        for (ResponseListener listener : listeners)
            listener.breakpointInfoObtained(lineTable);
    }

    void notifyBreakpointHit(long threadId, Location location) {
        for (ResponseListener listener : listeners)
            listener.breakPointHit(threadId, location);
    }

    void notifyFrameInfoObtained(long frameId) {
        for (ResponseListener listener : listeners)
            listener.frameIdObtained(frameId);
    }

}

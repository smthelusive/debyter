package smthelusive.debyter;

import smthelusive.debyter.domain.*;

import java.util.List;

public interface ResponseListener {
    void incomingPacket(ResponsePacket incomingPacket);
    void finish();
    void classIsLoaded(long refTypeId);
    void classAndMethodsInfoObtained(long threadId, long classId, List<AMethod> methods);
    void breakpointInfoObtained(LineTable lineTable);
    void breakPointHit(long threadId, Location location);
    void frameIdObtained(long frameId);
}

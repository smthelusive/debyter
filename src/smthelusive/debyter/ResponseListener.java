package smthelusive.debyter;

import smthelusive.debyter.domain.*;

public interface ResponseListener {
    void incomingPacket(ResponsePacket incomingPacket);

}

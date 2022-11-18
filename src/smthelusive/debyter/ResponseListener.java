package smthelusive.debyter;

import smthelusive.debyter.domain.ResponsePacket;

public interface ResponseListener {
    void incomingPacket(ResponsePacket incomingPacket);
    void finish();
}

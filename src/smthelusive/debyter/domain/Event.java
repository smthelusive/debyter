package smthelusive.debyter.domain;

public class Event {
    private byte eventKind;
    private int requestID;
    private long thread;
    private Location location;
    private int catchLocation;
    private String value;
    private String object;
    private long timeout;
    private boolean timedOut;
    private String exception;
    private byte refTypeTag;
    private long refTypeId;
    private long typeID;
    private String signature;
    private int	status;
    private int fieldID;

    public byte getEventKind() {
        return eventKind;
    }

    public long getRefTypeId() {
        return refTypeId;
    }

    public void setRefTypeId(long refTypeId) {
        this.refTypeId = refTypeId;
    }

    public void setEventKind(byte eventKind) {
        this.eventKind = eventKind;
    }

    public int getRequestID() {
        return requestID;
    }

    public void setRequestID(int requestID) {
        this.requestID = requestID;
    }

    public long getThread() {
        return thread;
    }

    public void setThread(long thread) {
        this.thread = thread;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public int getCatchLocation() {
        return catchLocation;
    }

    public void setCatchLocation(int catchLocation) {
        this.catchLocation = catchLocation;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public boolean isTimedOut() {
        return timedOut;
    }

    public void setTimedOut(boolean timedOut) {
        this.timedOut = timedOut;
    }

    public String getException() {
        return exception;
    }

    public void setException(String exception) {
        this.exception = exception;
    }

    public byte getRefTypeTag() {
        return refTypeTag;
    }

    public void setRefTypeTag(byte refTypeTag) {
        this.refTypeTag = refTypeTag;
    }

    public long getTypeID() {
        return typeID;
    }

    public void setTypeID(long typeID) {
        this.typeID = typeID;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getFieldID() {
        return fieldID;
    }

    public void setFieldID(int fieldID) {
        this.fieldID = fieldID;
    }

    @Override
    public String toString() {
        return "Event{" +
                "eventKind=" + eventKind +
                ", requestID=" + requestID +
                ", thread=" + thread +
                ", location=" + location +
                ", catchLocation=" + catchLocation +
                ", value='" + value + '\'' +
                ", object='" + object + '\'' +
                ", timeout=" + timeout +
                ", timedOut=" + timedOut +
                ", exception='" + exception + '\'' +
                ", refTypeTag=" + refTypeTag +
                ", refTypeId=" + refTypeId +
                ", typeID='" + typeID + '\'' +
                ", signature='" + signature + '\'' +
                ", status=" + status +
                ", fieldID=" + fieldID +
                '}';
    }
}

package smthelusive.debyter.domain;

import java.util.ArrayList;

public class ResponsePacket {
    // header:
    private int length;
    private int id;
    private byte flag;
    private short errorCode;
    // body:
    private byte suspendPolicy;
    private int events;
    private ArrayList<Event> eventsList;

    private int responseType;
    private ArrayList<AClass> classes;
    private ArrayList<AMethod> methods;
    private LineTable lineTable;

    public LineTable getLineTable() {
        return lineTable;
    }

    public void setLineTable(LineTable lineTable) {
        this.lineTable = lineTable;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public byte getFlag() {
        return flag;
    }

    public void setFlag(byte flag) {
        this.flag = flag;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(short errorCode) {
        this.errorCode = errorCode;
    }

    public byte getSuspendPolicy() {
        return suspendPolicy;
    }

    public void setSuspendPolicy(byte suspendPolicy) {
        this.suspendPolicy = suspendPolicy;
    }

    public int getEvents() {
        return events;
    }

    public void setEvents(int events) {
        this.events = events;
    }

    public ArrayList<Event> getEventsList() {
        return eventsList;
    }

    public void addEvent(Event event) {
        if (this.eventsList == null) eventsList = new ArrayList<>();
        eventsList.add(event);
    }

    public int getResponseType() {
        return responseType;
    }

    public void setResponseType(int responseType) {
        this.responseType = responseType;
    }

    public ArrayList<AMethod> getMethods() {
        return methods;
    }

    public ArrayList<AClass> getClasses() {
        return classes;
    }

    public void addClass(byte refTypeTag, long typeID, int status) {
        addClass(refTypeTag, typeID, null, status);
    }
    public void addClass(byte refTypeTag, long typeID, String signature, int status) {
        AClass aClass = new AClass(refTypeTag, typeID, signature, status);
        if (this.classes == null) classes = new ArrayList<>();
        classes.add(aClass);
    }

    public void addMethod(long methodId, String name, String signature, int modBits) {
        AMethod aMethod = new AMethod(methodId, name, signature, modBits);
        if (this.methods == null) methods = new ArrayList<>();
        methods.add(aMethod);
    }

    @Override
    public String toString() {
        return "ResponsePacket{" +
                "length=" + length +
                ", id=" + id +
                ", flag=" + flag +
                ", errorCode=" + errorCode +
                ", suspendPolicy=" + suspendPolicy +
                ", events=" + events +
                ", eventsList=" + eventsList +
                ", responseType=" + responseType +
                ", classes=" + classes +
                ", methods=" + methods +
                '}';
    }
}

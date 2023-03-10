package smthelusive.debyter.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    private EventType eventType;
    private ArrayList<AClass> classes;
    private ArrayList<AMethod> methods;
    private LineTable lineTable;

    private VariableTable variableTable;
    private final List<GenericVariable> genericVariables = new ArrayList<>();
    private final List<Long> activeThreads = new ArrayList<>();

    private int fieldIDSize;
    private int methodIDSize;
    private int objectIDSize;
    private int referenceTypeIDSize;
    private int frameIDSize;
    private long frameId;

    private byte[] bytecodes;

    private Location location;

    private String stringValue;

    public List<Long> getActiveThreads() {
        return activeThreads;
    }

    public void addActiveThread(long threadId) {
        activeThreads.add(threadId);
    }

    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    public List<GenericVariable> getGenericVariables() {
        return genericVariables;
    }

    public void addVariable(GenericVariable genericVariable) {
        genericVariables.add(genericVariable);
    }

    public byte[] getBytecodes() {
        return bytecodes;
    }

    public void setBytecodes(byte[] bytecodes) {
        this.bytecodes = bytecodes;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public long getFrameId() {
        return frameId;
    }

    public void setFrameId(long frameId) {
        this.frameId = frameId;
    }

    public LineTable getLineTable() {
        return lineTable;
    }

    public void setLineTable(LineTable lineTable) {
        this.lineTable = lineTable;
    }

    public VariableTable getVariableTable() {
        return variableTable;
    }

    public void setVariableTable(VariableTable variableTable) {
        this.variableTable = variableTable;
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

    public int getFieldIDSize() {
        return fieldIDSize;
    }

    public void setFieldIDSize(int fieldIDSize) {
        this.fieldIDSize = fieldIDSize;
    }

    public int getMethodIDSize() {
        return methodIDSize;
    }

    public void setMethodIDSize(int methodIDSize) {
        this.methodIDSize = methodIDSize;
    }

    public int getObjectIDSize() {
        return objectIDSize;
    }

    public void setObjectIDSize(int objectIDSize) {
        this.objectIDSize = objectIDSize;
    }

    public int getReferenceTypeIDSize() {
        return referenceTypeIDSize;
    }

    public void setReferenceTypeIDSize(int referenceTypeIDSize) {
        this.referenceTypeIDSize = referenceTypeIDSize;
    }

    public int getFrameIDSize() {
        return frameIDSize;
    }

    public void setFrameIDSize(int frameIDSize) {
        this.frameIDSize = frameIDSize;
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
                ", eventType=" + eventType +
                ", classes=" + classes +
                ", methods=" + methods +
                ", lineTable=" + lineTable +
                ", variableTable=" + variableTable +
                ", variableValues=" + genericVariables +
                ", fieldIDSize=" + fieldIDSize +
                ", methodIDSize=" + methodIDSize +
                ", objectIDSize=" + objectIDSize +
                ", referenceTypeIDSize=" + referenceTypeIDSize +
                ", frameIDSize=" + frameIDSize +
                ", frameId=" + frameId +
                ", bytecodes=" + Arrays.toString(bytecodes) +
                ", location=" + location +
                ", stringValue='" + stringValue + '\'' +
                '}';
    }
}

package smthelusive.debyter.domain;

import smthelusive.debyter.Utils;

import java.util.*;

public class CurrentState {
    private Location location;
    private byte[] bytecodes; // todo save this in the map with method ids so no need to request if this is already available
    private VariableTable variableTable; // todo save this in the map with method ids so no need to request if this is already available
    private long threadId;
    private long frameId = -1;

    private long arrayID = -1;

    public long getArrayID() {
        return arrayID;
    }

    public void setArrayID(long arrayID) {
        this.arrayID = arrayID;
    }

    private List<Long> activeThreads = new ArrayList<>();
    private final Map<Long, Set<AMethod>> methodsByClassId = new HashMap<>();

    private List<AClass> classes = new ArrayList<>();

    public List<Long> getActiveThreads() {
        return activeThreads;
    }

    public void addActiveThread(long threadId) {
        this.activeThreads.add(threadId);
    }

    public long getFrameId() {
        return frameId;
    }

    public void setFrameId(long frameId) {
        this.frameId = frameId;
    }

    public void setActiveThreads(List<Long> activeThreads) {
        this.activeThreads = activeThreads;
    }

    public List<AClass> getClasses() {
        return classes;
    }

    public void setClasses(List<AClass> classes) {
        this.classes = classes;
    }

    public void addClass(AClass aClass) {
        classes.add(aClass);
    }

    public Long getClassIdByName(String className) {
        return classes.stream()
                .filter(aClass -> aClass.signature().equals(Utils.getInternalRepresentationOfObjectType(className)))
                .map(AClass::typeID)
                .findAny()
                .orElse(0L);
    }

    public Long getMethodIdByClassAndName(Long classId, String methodName) {
        return methodsByClassId.get(classId).stream().filter(method -> method.name().equals(methodName))
                .map(AMethod::methodId).findAny().orElse(0L);
    }

    public boolean classMethodInfoAvailable(String className, String methodName) {
        return classes.stream().filter(aClass ->
                        aClass.signature().equals(Utils.getInternalRepresentationOfObjectType(className)))
                .flatMap(aClass -> Optional.ofNullable(methodsByClassId.get(aClass.typeID())).stream())
                .flatMap(Collection::stream)
                .anyMatch(method -> method.name().equals(methodName));
    }

    public boolean classInfoAvailable(String className) {
        return classes.stream().anyMatch(aClass -> aClass.signature().equals(Utils.getInternalRepresentationOfObjectType(className)));
    }


    public Map<Long, Set<AMethod>> getMethodsByClassId() {
        return methodsByClassId;
    }

    public Optional<Set<AMethod>> getMethodsOfClassId(long classId) {
        return Optional.ofNullable(getMethodsByClassId().get(classId));
    }

    public void addMethodForClassId(Long classId, AMethod method) {
        if (Optional.ofNullable(methodsByClassId.get(classId)).isEmpty()) {
            Set<AMethod> set = new HashSet<>();
            methodsByClassId.put(classId, set);
        }
        methodsByClassId.get(classId).add(method);
    }

    public boolean isNewLocationClassOrMethod(Location location) {
        if (location == null) return false;
        return (this.location == null || (
                this.location.methodId() != location.methodId() || this.location.classId() != location.classId()));
    }

    public Optional<Location> getLocation() {
        return Optional.ofNullable(location);
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Optional<byte[]> getBytecodes() {
        return Optional.ofNullable(bytecodes);
    }

    public void setBytecodes(byte[] bytecodes) {
        this.bytecodes = bytecodes;
    }

    public Optional<VariableTable> getVariableTable() {
        return Optional.ofNullable(variableTable);
    }

    public void setVariableTable(VariableTable variableTable) {
        this.variableTable = variableTable;
    }

    public long getThreadId() {
        return threadId;
    }

    public void setThreadId(long threadId) {
        this.threadId = threadId;
    }
}

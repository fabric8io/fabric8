package io.fabric8.apmagent;

public class MethodDescription {
    private final String className;
    private final String methodName;
    private final String description;
    private final String methodSignature;
    private final String fullMethodName;

    MethodDescription(String className, String methodName, String description) {
        this.className = className.replace('/', '.');
        this.methodName = methodName.replace('/', '.');
        this.description = description;
        this.methodSignature = getMethodSignature(methodName, description);
        this.fullMethodName = this.className + "@" + this.methodSignature;
    }

    static String getMethodSignature(String name, String description) {
        return name.replace('/', '.') + description;
    }

    public String getFullMethodName() {
        return fullMethodName;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodSignature() {
        return methodSignature;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getDescription() {
        return description;
    }
}

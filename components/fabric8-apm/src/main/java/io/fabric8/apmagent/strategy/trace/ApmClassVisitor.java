/**
 *  Copyright 2005-2016 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.apmagent.strategy.trace;

import io.fabric8.apmagent.ApmAgent;
import io.fabric8.apmagent.ClassInfo;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.objectweb.asm.Opcodes.ASM5;

public class ApmClassVisitor extends ClassVisitor {
    private static final Logger LOG = LoggerFactory.getLogger(ApmAgent.class);
    private final TraceStrategy traceStrategy;
    private final ClassInfo classInfo;

    public ApmClassVisitor(TraceStrategy traceStrategy, ClassVisitor cv, ClassInfo classInfo) {
        super(ASM5, cv);
        this.traceStrategy = traceStrategy;
        this.classInfo = classInfo;
    }

    public void visit(int version,
                      int access,
                      String name,
                      String signature,
                      String superName,
                      String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
    }

    public MethodVisitor visitMethod(int access,
                                     String name,
                                     String desc,
                                     String signature,
                                     String[] exceptions) {

        try {
            String methodDescription = getDescription(desc);
            classInfo.addMethod(name, methodDescription);

            if (canProfileMethod(name, desc) && traceStrategy.isAudit(classInfo.getClassName(), name)) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

                ApmMethodVisitor methodVisitor = new ApmMethodVisitor(mv, classInfo.getClassName(), name + methodDescription);
                classInfo.addTransformedMethod(name, methodDescription);
                return methodVisitor;
            }

        } catch (Throwable e) {
            e.printStackTrace();
            LOG.error("Failed to visitMethod " + name, e);
        }

        return super.visitMethod(access, name, desc, signature, exceptions);
    }

    private boolean canProfileMethod(String methodName, String methodDescriptor) {
        if (methodDescriptor != null) {

            Type[] parameterTypes = Type.getArgumentTypes(methodDescriptor);

            if (methodName.equals("<init>") || methodName.equals("<clinit>")) {
                return false;
            }
            if (methodName.startsWith("is") || methodName.startsWith("get") && (parameterTypes == null || parameterTypes.length == 0)) {
                return false;
            }
            return !(methodName.startsWith("set") && parameterTypes != null && parameterTypes.length == 1);
        }
        return false;
    }

    private String getDescription(String desc) {
        Type[] parameterTypes = Type.getArgumentTypes(desc);
        String result;
        if (parameterTypes == null || parameterTypes.length == 0) {
            result = "()";
        } else {
            result = "(";
            for (int i = 0; i < parameterTypes.length; i++) {
                result += parameterTypes[i].getClassName();
                if ((i + 1) < parameterTypes.length) {
                    result += ",";
                }
            }
            result += ")";
        }
        Type type = Type.getReturnType(desc);
        if (type == null) {
            result += " void";
        } else {
            result += " " + type.getClassName();
        }
        return result;
    }
}

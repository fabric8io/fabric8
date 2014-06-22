/*
 * Copyright 2005-2014 Red Hat, Inc.
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.fabric8.apmagent;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.objectweb.asm.Opcodes.ASM5;

public class ApmClassVisitor extends ClassVisitor {
  private static final Logger LOG = LoggerFactory.getLogger(ApmAgent.class);
  final String id;

  public ApmClassVisitor(ClassVisitor cv, String classId) {
    super(ASM5, cv);
    this.id = classId;
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
      if (canProfileMethod(name, desc) && ApmAgent.INSTANCE.getConfiguration().isAudit(id, name)) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        Type[] parameterTypes = Type.getArgumentTypes(desc);
        String str = "";
        if (parameterTypes == null || parameterTypes.length == 0) {
          str = "()";
        } else {
          str = "(";
          for (int i = 0; i < parameterTypes.length; i++) {
            str += parameterTypes[i].getClassName();
            if ((i + 1) < parameterTypes.length) {
              str += ",";
            }
          }
          str += ")";
        }
        Type type = Type.getReturnType(desc);
        if (type == null) {
          str += " void";
        } else {
          str += " " + type.getClassName();
        }
        ApmMethodVisitor methodVisitor = new ApmMethodVisitor(mv, id, name, str);
        return methodVisitor;
      }

    } catch (Throwable e) {
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
      if (methodName.startsWith("set") && parameterTypes != null && parameterTypes.length == 1) {
        return false;
      }
      return true;
    }
    return false;
  }
}

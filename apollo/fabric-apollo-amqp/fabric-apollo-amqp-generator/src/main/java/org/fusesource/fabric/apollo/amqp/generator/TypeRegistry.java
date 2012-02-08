/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.fabric.apollo.amqp.generator;

import com.sun.codemodel.*;
import org.fusesource.hawtbuf.Buffer;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class TypeRegistry {

    private String typeRegistryName;
    private JCodeModel cm;

    private JDefinedClass typeRegistry;

    JClass mapByteClass;
    JClass hashMapByteClass;
    JClass mapLongClass;
    JClass mapBufferClass;
    JClass hashMapLongClass;
    JClass hashMapBufferClass;

    public TypeRegistry(JCodeModel cm, String typeRegistry) throws JClassAlreadyExistsException {
        this.cm = cm;
        this.typeRegistryName = typeRegistry;

        mapLongClass = cm.ref(Map.class).narrow(BigInteger.class, Class.class);
        mapBufferClass = cm.ref(Map.class).narrow(Buffer.class, Class.class);
        hashMapLongClass = cm.ref(HashMap.class).narrow(BigInteger.class, Class.class);
        hashMapBufferClass = cm.ref(HashMap.class).narrow(Buffer.class, Class.class);
        mapByteClass = cm.ref(Map.class).narrow(Byte.class, Class.class);
        hashMapByteClass = cm.ref(HashMap.class).narrow(Byte.class, Class.class);

        init();
    }

    public void init() throws JClassAlreadyExistsException {
        typeRegistry = cm._class(typeRegistryName);

        typeRegistry.field(JMod.PUBLIC | JMod.FINAL | JMod.STATIC, cm.BYTE, "DESCRIBED_FORMAT_CODE", JExpr.direct("0x00"));
        typeRegistry.field(JMod.PUBLIC | JMod.FINAL | JMod.STATIC, cm.BYTE, "NULL_FORMAT_CODE", JExpr.direct("0x40"));

        JFieldVar singleton = typeRegistry.field(JMod.PROTECTED | JMod.FINAL | JMod.STATIC, (JType) typeRegistry, "SINGLETON", JExpr._new(typeRegistry));

        JFieldVar primitiveFormatCodeMap = typeRegistry.field(JMod.PROTECTED | JMod.FINAL, mapByteClass, "primitiveFormatCodeMap", JExpr._new(hashMapByteClass));
        JFieldVar formatCodeMap = typeRegistry.field(JMod.PROTECTED | JMod.FINAL, mapLongClass, "formatCodeMap", JExpr._new(hashMapLongClass));
        JFieldVar symbolicCodeMap = typeRegistry.field(JMod.PROTECTED | JMod.FINAL, mapBufferClass, "symbolicCodeMap", JExpr._new(hashMapBufferClass));

        JMethod singletonAccessor = typeRegistry.method(JMod.PUBLIC | JMod.STATIC, typeRegistry, "instance");
        singletonAccessor.body()._return(JExpr.ref("SINGLETON"));

        JMethod formatCodeMapGetter = typeRegistry.method(JMod.PUBLIC, mapLongClass, "getFormatCodeMap");
        formatCodeMapGetter.body()._return(JExpr.ref("formatCodeMap"));

        JMethod symbolicCodeMapGetter = typeRegistry.method(JMod.PUBLIC, mapBufferClass, "getSymbolicCodeMap");
        symbolicCodeMapGetter.body()._return(JExpr.ref("symbolicCodeMap"));

        JMethod primitiveFormatCodeGetter = typeRegistry.method(JMod.PUBLIC, mapByteClass, "getPrimitiveFormatCodeMap");
        primitiveFormatCodeGetter.body()._return(JExpr.ref("primitiveFormatCodeMap"));
    }

    public JDefinedClass cls() {
        return typeRegistry;
    }


}

/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.apollo.amqp.generator;

import com.sun.codemodel.*;
import org.fusesource.hawtbuf.Buffer;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class TypeRegistry {

    private String typeRegistryName;
    private JCodeModel cm;

    private JDefinedClass typeRegistry;

    JClass mapLongClass;
    JClass mapBufferClass;
    JClass hashMapLongClass;
    JClass hashMapBufferClass;

    public TypeRegistry(JCodeModel cm, String typeRegistry) throws JClassAlreadyExistsException {
        this.cm = cm;
        this.typeRegistryName = typeRegistry;

        mapLongClass = cm.ref(Map.class).narrow(Long.class, Class.class);
        mapBufferClass = cm.ref(Map.class).narrow(Buffer.class, Class.class);
        hashMapLongClass = cm.ref(HashMap.class).narrow(Long.class, Class.class);
        hashMapBufferClass = cm.ref(HashMap.class).narrow(Buffer.class, Class.class);

        init();
    }

    public void init() throws JClassAlreadyExistsException {
        typeRegistry = cm._class(typeRegistryName);

        typeRegistry.field(JMod.PUBLIC | JMod.FINAL | JMod.STATIC, cm.BYTE, "DESCRIBED_FORMAT_CODE", JExpr.direct("0x00"));
        typeRegistry.field(JMod.PUBLIC | JMod.FINAL | JMod.STATIC, cm.BYTE, "NULL_FORMAT_CODE", JExpr.direct("0x40"));

        JFieldVar singleton = typeRegistry.field(JMod.PROTECTED | JMod.FINAL | JMod.STATIC, (JType) typeRegistry, "SINGLETON", JExpr._new(typeRegistry));
        JFieldVar formatCodeMap = typeRegistry.field(JMod.PROTECTED | JMod.FINAL, mapLongClass, "formatCodeMap", JExpr._new(hashMapLongClass));
        JFieldVar symbolicCodeMap = typeRegistry.field(JMod.PROTECTED | JMod.FINAL, mapBufferClass, "symbolicCodeMap", JExpr._new(hashMapBufferClass));

        JMethod singletonAccessor = typeRegistry.method(JMod.PUBLIC | JMod.STATIC, typeRegistry, "instance");
        singletonAccessor.body()._return(JExpr.ref("SINGLETON"));

        JMethod formatCodeMapGetter = typeRegistry.method(JMod.PUBLIC, mapLongClass, "getFormatCodeMap");
        formatCodeMapGetter.body()._return(JExpr.ref("formatCodeMap"));

        JMethod symbolicCodeMapGetter = typeRegistry.method(JMod.PUBLIC, mapBufferClass, "getSymbolicCodeMap");
        symbolicCodeMapGetter.body()._return(JExpr.ref("symbolicCodeMap"));
    }

    public JDefinedClass cls() {
        return typeRegistry;
    }


}

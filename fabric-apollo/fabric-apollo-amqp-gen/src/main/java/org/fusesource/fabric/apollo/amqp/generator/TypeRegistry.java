/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fusemq.amqp.generator;

import java.util.Collection;
import java.util.HashMap;

public class TypeRegistry {

    private static final HashMap<String, JavaTypeMapping> JAVA_TYPE_MAP = new HashMap<String, JavaTypeMapping>();
    private static final HashMap<String, AmqpClass> GENERATED_TYPE_MAP = new HashMap<String, AmqpClass>();

    static final void init(Generator generator) {

        // Add in the wildcard type:
        AmqpClass any = new AmqpType("*", generator.getPackagePrefix() + ".types.AmqpType");
        GENERATED_TYPE_MAP.put("*", any);
        JAVA_TYPE_MAP.put("*", any.typeMapping);

        AmqpClass multiple = new Multiple("multiple", generator.getPackagePrefix() + ".types", "Multiple");
        GENERATED_TYPE_MAP.put("multiple", multiple);

        //
        JAVA_TYPE_MAP.put("boolean", new JavaTypeMapping("boolean", "java.lang.Boolean", "boolean"));
        JAVA_TYPE_MAP.put("ubyte", new JavaTypeMapping("ubyte", "java.lang.Short", "short"));
        JAVA_TYPE_MAP.put("ushort", new JavaTypeMapping("ushort", "java.lang.Integer", "int"));
        JAVA_TYPE_MAP.put("uint", new JavaTypeMapping("uint", "java.lang.Long", "long"));
        JAVA_TYPE_MAP.put("ulong", new JavaTypeMapping("ulong", "java.math.BigInteger"));
        JAVA_TYPE_MAP.put("byte", new JavaTypeMapping("byte", "java.lang.Byte", "byte"));
        JAVA_TYPE_MAP.put("short", new JavaTypeMapping("short", "java.lang.Short", "short"));
        JAVA_TYPE_MAP.put("int", new JavaTypeMapping("int", "java.lang.Integer", "int"));
        JAVA_TYPE_MAP.put("long", new JavaTypeMapping("long", "java.lang.Long", "long"));
        JAVA_TYPE_MAP.put("float", new JavaTypeMapping("float", "java.lang.Float", "float"));
        JAVA_TYPE_MAP.put("double", new JavaTypeMapping("double", "java.lang.Double", "double"));
        JAVA_TYPE_MAP.put("decimal32", new JavaTypeMapping("decimal32", "java.math.BigDecimal"));
        JAVA_TYPE_MAP.put("decimal64", new JavaTypeMapping("decimal64", "java.math.BigDecimal"));
        JAVA_TYPE_MAP.put("decimal128", new JavaTypeMapping("decimal128", "java.math.BigDecimal"));
        JAVA_TYPE_MAP.put("char", new JavaTypeMapping("char", "java.lang.Integer", "int"));
        JAVA_TYPE_MAP.put("timestamp", new JavaTypeMapping("timestamp", "java.util.Date"));
        JAVA_TYPE_MAP.put("uuid", new JavaTypeMapping("uuid", "java.util.UUID"));
        JAVA_TYPE_MAP.put("binary", new JavaTypeMapping("binary", "org.fusesource.hawtbuf.Buffer"));
        JAVA_TYPE_MAP.put("string", new JavaTypeMapping("string", "java.lang.String"));
        JAVA_TYPE_MAP.put("symbol", new JavaTypeMapping("symbol", "java.lang.String"));
        JAVA_TYPE_MAP.put("list", new JavaTypeMapping("list", generator.getPackagePrefix() + ".types.IAmqpList", false, "<" + any.getJavaType() + ">"));
        JAVA_TYPE_MAP.put("multiple", new JavaTypeMapping("list", generator.getPackagePrefix() + ".types.IAmqpList", false, "<" + any.getJavaType() + ">"));
        JAVA_TYPE_MAP.put("map", new JavaTypeMapping("map", generator.getPackagePrefix() + ".types.IAmqpMap", false, "<" + any.getJavaType() + ", " + any.getJavaType() + ">"));
        JAVA_TYPE_MAP.put("null", new JavaTypeMapping("null", "java.lang.Object"));

    }

    public static String dump() {
        String ret = "\n" + Utils.dumpMap(JAVA_TYPE_MAP, "Java Type Map");
        return ret;
    }

    public static JavaTypeMapping getJavaTypeMapping(String name) throws UnknownTypeException {
        JavaTypeMapping mapping = JAVA_TYPE_MAP.get(name);
        if (mapping == null) {
            // Try to find a class that defines it:
            AmqpClass amqpClass = GENERATED_TYPE_MAP.get(name);
            if (amqpClass != null) {
                mapping = amqpClass.typeMapping;
            }
            if (mapping == null) {
                throw new UnknownTypeException(name);
            }
        }
        return mapping;
    }

    public static boolean isPrimitive(String name) {
        return JAVA_TYPE_MAP.containsKey(name);
    }

    public static String getBasePrimitiveType(String type) throws UnknownTypeException {
        AmqpClass amqpClass = GENERATED_TYPE_MAP.get(type);
        if ( amqpClass.isPrimitive() ) {
            return amqpClass.getName();
        }
        if ( isPrimitive(amqpClass.getRestrictedType()) ) {
            return amqpClass.getRestrictedType();
        } else {
            getBasePrimitiveType(amqpClass.getRestrictedType());
        }
        throw new UnknownTypeException(type);
    }

    public static AmqpClass any() {
        return GENERATED_TYPE_MAP.get("*");
    }

    public static AmqpClass resolveAmqpClass(AmqpField amqpField) throws UnknownTypeException {
        return resolveAmqpClass(amqpField.getType());
    }

    public static AmqpClass resolveAmqpClass(String type) throws UnknownTypeException {
        AmqpClass amqpClass = GENERATED_TYPE_MAP.get(type);
        if (amqpClass == null) {
            throw new UnknownTypeException("Type " + type + " not found");
        }
        return amqpClass;
    }

    public static String getJavaType(AmqpField field) throws UnknownTypeException {
        return getJavaType(field.getType());
    }

    public static String getJavaType(String type) throws UnknownTypeException {
        AmqpClass amqpClass = GENERATED_TYPE_MAP.get(type);
        if (amqpClass == null) {
            throw new UnknownTypeException("Type " + type + " not found");
        }

        // Replace with restricted type:
        if (amqpClass.isRestricted()) {
            return getJavaType(amqpClass.getRestrictedType());
        }

        if (amqpClass.isPrimitive()) {
            JavaTypeMapping mapping = JAVA_TYPE_MAP.get(amqpClass.getName());
            if (mapping == null) {
                throw new UnknownTypeException("Primitive Type " + type + " not found");
            }
            return mapping.javaType;
        }

        return amqpClass.getJavaType();
    }

    public static Collection<AmqpClass> getGeneratedTypes() {
        return GENERATED_TYPE_MAP.values();
    }

    public static void addType(AmqpClass amqpClass) {
        GENERATED_TYPE_MAP.put(amqpClass.getName(), amqpClass);
    }

    public static class JavaTypeMapping {

        private String amqpType;
        private String shortName;
        private String packageName;
        private String fullName;
        private String javaType;
        private String primitiveType;

        boolean array;
        String generic;

        JavaTypeMapping(String amqpType, String fullName, boolean array, String generic) {
            this(amqpType, fullName);
            this.array = array;
            setGeneric(generic);
            if (array) {
                javaType = javaType + " []";
            }
        }

        JavaTypeMapping(String amqpType, String fullName, String primitiveType) {
            this.amqpType = amqpType;
            this.fullName = fullName;
            this.primitiveType = primitiveType;
            this.packageName = fullName.substring(0, fullName.lastIndexOf("."));
            this.shortName = fullName.substring(fullName.lastIndexOf(".") + 1);
            this.javaType = shortName;
        }

        JavaTypeMapping(String amqpType, String fullName) {
            this.amqpType = amqpType;
            this.fullName = fullName;
            this.packageName = fullName.substring(0, fullName.lastIndexOf("."));
            this.shortName = fullName.substring(fullName.lastIndexOf(".") + 1);
            this.javaType = shortName;
        }

        JavaTypeMapping(String amqpType, String packageName, String className, boolean inner) {
            this.amqpType = amqpType;
            this.fullName = packageName + "." + className;
            this.packageName = packageName;
            this.javaType = className;
            if (inner) {
                this.javaType = className;
                this.shortName = className.substring(className.lastIndexOf(".") + 1);
            }
        }

        public boolean hasPrimitiveType() {
            return primitiveType != null;
        }

        public String getPrimitiveType() {
            if (primitiveType == null) {
                return javaType;
            }
            return primitiveType;
        }

        public String getAmqpType() {
            return amqpType;
        }

        public void setAmqpType(String amqpType) {
            this.amqpType = amqpType;
        }

        public String getClassName() {
            return shortName;
        }

        public String getShortName() {
            return shortName;
        }

        public void setShortName(String shortName) {
            this.shortName = shortName;
        }

        public String getPackageName() {
            return packageName;
        }

        public void setPackageName(String packageName) {
            this.packageName = packageName;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getJavaType() {
            return javaType;
        }

        private String parameratize(String str, String... generics) {
            if (generic != null) {
                if (generics != null && generics.length > 0) {
                    boolean first = true;
                    for (String g : generics) {
                        if (!first) {
                            str += ", " + g;
                        } else {
                            first = false;
                            str += "<" + g;
                        }
                    }
                    return str + ">";

                } else {
                    return javaType + generic;
                }

            }
            return str;
        }

        public void setJavaType(String javaType) {
            this.javaType = javaType;
        }

        public boolean isArray() {
            return array;
        }

        public void setArray(boolean array) {
            this.array = array;
        }

        public String getGeneric() {
            return generic;
        }

        public void setGeneric(String generic) {
            this.generic = generic;
            javaType = shortName + generic;
        }

        public String getFullVersionMarshallerName(Generator generator) {
            return generator.getMarshallerPackage() + "." + shortName + "Marshaller";
        }

        public String getImport() {
            return fullName;
        }

        public String toString() {
            return getJavaType();
        }
    }

    public static class AmqpType extends AmqpClass {
        AmqpType(String amqpName, String fullName) {
            super.typeMapping = new JavaTypeMapping(amqpName, fullName, false, "<?, ?>");
            super.name = amqpName;
            super.setPrimitive(true);
            super.handcoded = true;
            super.valueMapping = typeMapping;
        }
    }

    public static class Multiple extends AmqpClass {
        Multiple(String amqpName, String packageName, String javaName) {
            super.typeMapping = new JavaTypeMapping(amqpName, packageName, javaName, false);
            super.name = amqpName;
            super.handcoded = true;
            super.primitive = false;
            super.descriptor = new AmqpDescriptor();
            super.descriptor.setSymbolicName("amqp:" + amqpName + ":list");
            super.descriptor.setFormatCode("0x41");
            super.beanMapping = new JavaTypeMapping(amqpName, packageName, javaName + "." + javaName + "Bean", true);
            super.bufferMapping = new JavaTypeMapping(amqpName, packageName, javaName + "." + javaName + "Buffer", true);
        }
    }

}

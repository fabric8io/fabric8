/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.api.test;

import io.fabric8.api.scr.AbstractFieldInjectionComponent;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;

import java.util.Arrays;
import java.util.List;

@Component(name = "io.fabric8.api.test.dummy", metatype = true)
public class DummyComponent extends AbstractFieldInjectionComponent {
    public static final byte DEFAULT_BYTE = 4;
    public static final char DEFAULT_CHAR = 'X';
    public static final short DEFAULT_SHORT = 12345;
    public static final int DEFAULT_INT = 1234567;
    public static final long DEFAULT_LONG = 1234567890123L;
    public static final float DEFAULT_FLOAT = 10.99f;
    public static final double DEFAULT_DOUBLE = 12345679.12345678;

    @Property(name = "name")
    private String name;
    @Property(name = "stringArray")
    private String[] stringArray;
    @Property(name = "listArray")
    private List<String> listArray;
    @Property(name = "stringArrayD", value="list2")
    private String[] stringArrayD;
    @Property(name = "listArrayD", value = "array2")
    private List<String> listArrayD;

    @Property(name = "propBool")
    private boolean propBool;
    @Property(name = "propBoolDefault", boolValue = true)
    private boolean propBoolDefault;

    @Property(name = "propBoolean")
    private Boolean propBoolean;
    @Property(name = "propBooleanDefault", boolValue = true)
    private Boolean propBooleanDefault;

    @Property(name = "propBoolean2")
    private Boolean propBoolean2;

    @Property(name = "propByte")
    private byte propByte;
    @Property(name = "propByteDefault", byteValue = DEFAULT_BYTE)
    private byte propByteDefault;

    @Property(name = "propChar")
    private char propChar;
    @Property(name = "propCharDefault", charValue = DEFAULT_CHAR)
    private char propCharDefault;

    @Property(name = "propCharacter")
    private Character propCharacter;
    @Property(name = "propCharacterDefault", charValue = DEFAULT_CHAR)
    private Character propCharacterDefault;

    @Property(name = "propShort")
    private short propShort;
    @Property(name = "propShortDefault", shortValue = DEFAULT_SHORT)
    private short propShortDefault;

    @Property(name = "propInt")
    private int propInt;
    @Property(name = "propIntDefault", intValue = DEFAULT_INT)
    private int propIntDefault;

    @Property(name = "propInteger")
    private Integer propInteger;
    @Property(name = "propIntegerDefault", intValue = DEFAULT_INT)
    private Integer propIntegerDefault;

    @Property(name = "propLong")
    private long propLong;
    @Property(name = "propLongDefault", longValue = DEFAULT_LONG)
    private long propLongDefault;

    @Property(name = "propFloat")
    private float propFloat;
    @Property(name = "propFloatDefault", floatValue = DEFAULT_FLOAT)
    private float propFloatDefault;

    @Property(name = "propDouble")
    private double propDouble;
    @Property(name = "propDoubleDefault", doubleValue = DEFAULT_DOUBLE)
    private double propDoubleDefault;

    @Property(name = "propDoubleObject")
    private Double propDoubleObject;
    @Property(name = "propDoubleObjectDefault", doubleValue = DEFAULT_DOUBLE)
    private Double propDoubleObjectDefault;

    @Override
    public String toString() {
        return "DummyComponent{" +
                "name='" + name + '\'' +
                ", stringArray=" + Arrays.toString(stringArray) +
                ", listArray=" + listArray +
                ", stringArrayD=" + Arrays.toString(stringArrayD) +
                ", listArrayD=" + listArrayD +
                ", propBool=" + propBool +
                ", propBoolDefault=" + propBoolDefault +
                ", propBoolean=" + propBoolean +
                ", propBooleanDefault=" + propBooleanDefault +
                ", propBoolean2=" + propBoolean2 +
                ", propByte=" + propByte +
                ", propByteDefault=" + propByteDefault +
                ", propChar=" + propChar +
                ", propCharDefault=" + propCharDefault +
                ", propCharacter=" + propCharacter +
                ", propCharacterDefault=" + propCharacterDefault +
                ", propShort=" + propShort +
                ", propShortDefault=" + propShortDefault +
                ", propInt=" + propInt +
                ", propIntDefault=" + propIntDefault +
                ", propInteger=" + propInteger +
                ", propIntegerDefault=" + propIntegerDefault +
                ", propLong=" + propLong +
                ", propLongDefault=" + propLongDefault +
                ", propFloat=" + propFloat +
                ", propFloatDefault=" + propFloatDefault +
                ", propDouble=" + propDouble +
                ", propDoubleDefault=" + propDoubleDefault +
                ", propDoubleObject=" + propDoubleObject +
                ", propDoubleObjectDefault=" + propDoubleObjectDefault +
                '}';
    }

    public String getName() {
        return name;
    }

    public String[] getStringArray() {
        return stringArray;
    }

    public List<String> getListArray() {
        return listArray;
    }

    public String[] getStringArrayD() {
        return stringArrayD;
    }

    public List<String> getListArrayD() {
        return listArrayD;
    }

    public boolean isPropBool() {
        return propBool;
    }

    public boolean isPropBoolDefault() {
        return propBoolDefault;
    }

    public Boolean getPropBoolean() {
        return propBoolean;
    }

    public Boolean getPropBooleanDefault() {
        return propBooleanDefault;
    }

    public Boolean getPropBoolean2() {
        return propBoolean2;
    }

    public byte getPropByte() {
        return propByte;
    }

    public byte getPropByteDefault() {
        return propByteDefault;
    }

    public char getPropChar() {
        return propChar;
    }

    public char getPropCharDefault() {
        return propCharDefault;
    }

    public Character getPropCharacter() {
        return propCharacter;
    }

    public Character getPropCharacterDefault() {
        return propCharacterDefault;
    }

    public short getPropShort() {
        return propShort;
    }

    public short getPropShortDefault() {
        return propShortDefault;
    }

    public int getPropInt() {
        return propInt;
    }

    public int getPropIntDefault() {
        return propIntDefault;
    }

    public Integer getPropInteger() {
        return propInteger;
    }

    public Integer getPropIntegerDefault() {
        return propIntegerDefault;
    }

    public long getPropLong() {
        return propLong;
    }

    public long getPropLongDefault() {
        return propLongDefault;
    }

    public float getPropFloat() {
        return propFloat;
    }

    public float getPropFloatDefault() {
        return propFloatDefault;
    }

    public double getPropDouble() {
        return propDouble;
    }

    public double getPropDoubleDefault() {
        return propDoubleDefault;
    }

    public Double getPropDoubleObject() {
        return propDoubleObject;
    }

    public Double getPropDoubleObjectDefault() {
        return propDoubleObjectDefault;
    }
}

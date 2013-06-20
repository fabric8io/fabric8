package org.fusesource.process.fabric.child.support;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.collect.Maps;

import java.util.Map;

public enum ByteToStringValues implements Function<Map<String, byte[]>, Map<String, String>> {

    INSTANCE;

    @Override
    public Map<String, String> apply(java.util.Map<String, byte[]> input) {
        return Maps.transformValues(input, new Function<byte[], String>() {
            @Override
            public String apply(byte[] input) {
                return new String(input, Charsets.UTF_8);
            }
        });
    }
}

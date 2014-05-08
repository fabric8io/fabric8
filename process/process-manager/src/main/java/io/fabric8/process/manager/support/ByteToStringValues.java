/**
 *  Copyright 2005-2014 Red Hat, Inc.
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
package io.fabric8.process.manager.support;

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

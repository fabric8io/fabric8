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
package io.fabric8.cxf.registry;

import io.fabric8.internal.JsonHelper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 */
public class JsonEncodeTest {
    @Test
    public void testJsonEncoding() throws Exception {
        String objectName = "org.apache.cxf:bus.id=rest-cxf515624438,type=Bus.Service.Endpoint,service=\"{http://rest.fuse.quickstarts.jboss.org/}CustomerService\",port=\"CustomerService\",instance.id=1776063910";
        String expectedValue = "\"org.apache.cxf:bus.id=rest-cxf515624438,type=Bus.Service.Endpoint,service=\\\"{http://rest.fuse.quickstarts.jboss.org/}CustomerService\\\",port=\\\"CustomerService\\\",instance.id=1776063910\"";
        String jsonValue = JsonHelper.jsonEncodeString(objectName);
        assertEquals("encoded JSON value", expectedValue, jsonValue);
    }

}

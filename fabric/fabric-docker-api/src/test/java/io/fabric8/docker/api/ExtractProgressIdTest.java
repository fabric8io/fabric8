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
package io.fabric8.docker.api;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 */
@Ignore("[FABRIC-1092] Fix Docker API tests")
public class ExtractProgressIdTest {
    @Test
    public void testExtractId() throws Exception {
        String progressJson = " {\"status\":\"Pulling repository base\"}\n" +
                "{\"status\":\"Pulling image (ubuntu-quantl) from base\",\"progressDetail\":{},\"id\":\"b750fe79269d\"}{\"status\":\"Pulling image (ubuntu-quantl) from base, endpoint: https://cdn-registry-1.docker.io/v1/\",\"progressDetail\":{},\"id\":\"b750fe79269d\"}{\"status\":\"Pulling dependent layers\",\"progressDetail\":{},\"id\":\"b750fe79269d\"}{\"status\":\"Download complete\",\"progressDetail\":{},\"id\":\"27cf78414709\"}{\"status\":\"Download complete\",\"progressDetail\":{},\"id\":\"b750fe79269d\"}{\"status\":\"Download complete\",\"progressDetail\":{},\"id\":\"b750fe79269d\"}";

        String id = Dockers.extractLastProgressId(progressJson);
        assertEquals("expected progress id", "b750fe79269d", id);
    }

}

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

package io.fabric8.launcher.api;

import org.junit.Test;

import java.io.InputStream;

import static org.junit.Assert.*;


/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */

public class XmlCodecTest {

    private InputStream resource(String path) {
        return getClass().getResourceAsStream(path);
    }

    @Test
    public void unmarshalling() throws Exception {
        ServiceDTO dto = (ServiceDTO) XmlCodec.decode(ServiceDTO.class, resource("example.xml"));
        assertNotNull(dto);
        assertEquals("example", dto.id);
        assertEquals(2, dto.start.size());
        assertEquals(2, dto.stop.size());
    }


    @Test
    public void marshalling() throws Exception {
        ServiceDTO dto = new ServiceDTO();
        dto.id = "test";
        dto.pid_file = "/var/run/test.pid";
        XmlCodec.encode(dto, System.out, true);

    }

}

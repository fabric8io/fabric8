/**
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

package org.apache.activemq.apollo.mqtt.dto;

import org.apache.activemq.apollo.dto.*;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;

import static junit.framework.Assert.*;
import static junit.framework.Assert.assertEquals;


/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */

public class XmlCodecTest {

    private InputStream resource(String path) {
        return getClass().getResourceAsStream(path);
    }

    @Test
    public void unmarshalling() throws Exception {
        BrokerDTO dto = XmlCodec.decode(BrokerDTO.class, resource("simple.xml"));
        assertNotNull(dto);

        assertEquals(1, dto.connectors.size());
        AcceptingConnectorDTO connector = (AcceptingConnectorDTO)dto.connectors.get(0);
        assertEquals(1, connector.protocols.size());
        ProtocolDTO mqtt = connector.protocols.get(0);
        assertTrue(mqtt instanceof MqttDTO);

    }

}

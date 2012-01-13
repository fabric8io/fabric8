/**
 * Copyright (C) 2010-2012, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
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

/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.apollo.cluster.dto;

import org.apache.activemq.apollo.dto.AcceptingConnectorDTO;
import org.apache.activemq.apollo.dto.BrokerDTO;
import org.apache.activemq.apollo.dto.VirtualHostDTO;
import org.apache.activemq.apollo.dto.XmlCodec;
import org.junit.Test;

import javax.xml.bind.JAXBException;
import java.io.InputStream;

import static junit.framework.Assert.*;


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
        assertTrue( dto.virtual_hosts.get(0) instanceof ClusterVirtualHostDTO);
    }

    @Test
    public void marshalling() throws JAXBException {
      BrokerDTO broker = new BrokerDTO();

      ClusterVirtualHostDTO host = new ClusterVirtualHostDTO();
      host.id = "vh-local";
      host.host_names.add("localhost");
      host.host_names.add("example.com");
      broker.virtual_hosts.add(host);

      ClusterConnectorDTO connector = new ClusterConnectorDTO();
      connector.id = "port-61616";
      broker.connectors.add(connector);

      XmlCodec.encode(broker, System.out, true);

    }
}

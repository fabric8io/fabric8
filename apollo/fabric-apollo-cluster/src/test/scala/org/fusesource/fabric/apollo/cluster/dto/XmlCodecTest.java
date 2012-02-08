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

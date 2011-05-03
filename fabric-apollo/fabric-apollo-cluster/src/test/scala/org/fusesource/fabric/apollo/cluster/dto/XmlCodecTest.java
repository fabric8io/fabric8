/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fusemq.cluster.dto;

import org.apache.activemq.apollo.dto.VirtualHostDTO;
import org.apache.activemq.apollo.dto.XmlCodec;
import org.junit.Test;

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
        ClusterBrokerDTO dto = XmlCodec.decode(ClusterBrokerDTO.class, resource("simple.xml"));
        assertNotNull(dto);
        VirtualHostDTO host = dto.virtual_hosts.get(0);
        assertNotNull(host.router);
        assertTrue( host.router instanceof ClusterRouterDTO);
    }


}

/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.launcher.api;

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

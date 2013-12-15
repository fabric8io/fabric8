/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.fabric8.agent.resolver;

import junit.framework.TestCase;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.cm.ConfigurationAdmin;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

public class ResourceBuilderTest extends TestCase {

    public void testParser() throws IOException, BundleException {
        ClassLoader loader = ConfigurationAdmin.class.getClassLoader();
        String resource = ConfigurationAdmin.class.getName().replace('.', '/') + ".class";
        URL url = loader.getResource(resource);
        if (url.getProtocol().equals("jar")) {
            String path = url.getPath();
            resource = path.substring(0, path.indexOf('!'));
            resource = URI.create(resource).getPath();
        }
        JarFile jar = new JarFile(resource);
        Attributes attributes = jar.getManifest().getMainAttributes();
        Map<String, String> headers = new HashMap<String, String>();
        for (Map.Entry key : attributes.entrySet()) {
            headers.put(key.getKey().toString(), key.getValue().toString());
        }

        Resource res = ResourceBuilder.build(url.toString(), headers);
        System.out.println("Capabilities");
        for (Capability cap : res.getCapabilities(null)) {
            System.out.println("    " + cap.toString());
        }
        System.out.println("Requirements");
        for (Requirement req : res.getRequirements(null)) {
            System.out.println("    " + req.toString());
        }
    }

    public void testMandatory() throws BundleException {
        List<Capability> caps = ResourceBuilder.parseExport(null, "bsn", Version.emptyVersion, "com.acme.foo; company=ACME; security=false; mandatory:=security");
        List<Requirement> reqs1 = ResourceBuilder.parseImport(null, "com.acme.foo;company=ACME");
        List<Requirement> reqs2 = ResourceBuilder.parseImport(null, "com.acme.foo;company=ACME;security=true");
        List<Requirement> reqs3 = ResourceBuilder.parseImport(null, "com.acme.foo;company=ACME;security=false");

        assertEquals(1, caps.size());
        assertEquals(1, reqs1.size());
        assertEquals(1, reqs2.size());
        assertEquals(1, reqs3.size());
        System.out.println(caps.get(0));
        System.out.println(reqs1.get(0));
        System.out.println(reqs2.get(0));
        System.out.println(reqs3.get(0));
        assertFalse(((RequirementImpl) reqs1.get(0)).matches(caps.get(0)));
        assertFalse(((RequirementImpl) reqs2.get(0)).matches(caps.get(0)));
        assertTrue(((RequirementImpl) reqs3.get(0)).matches(caps.get(0)));
    }

    public void testTypedAttributes() throws Exception {
        String header = "com.acme.dictionary; from:String=nl; to=de; version:Version=3.4; indices:List<Long>=\" 23 , 45 \", " +
                        "com.acme.dictionary; from:String=de; to=nl; version:Version=4.1, " +
                        "com.acme.ip2location;country:List<String>=\"nl,be,fr,uk\";version:Version=1.3, " +
                        "com.acme.seps;        tokens:List<String>=\";,\\\",\\,\"";

        List<Capability> caps = ResourceBuilder.parseCapability(null, header);

        System.out.println("Header: " + header);
        for (Capability bc : caps) {
            System.out.println(bc);
            List<Capability> cap = ResourceBuilder.parseCapability(null, bc.toString());
            assertEquals(1, cap.size());
            assertEquals(cap.get(0).toString(), bc.toString());
            assertEquals(cap.get(0), bc);
        }
    }

}

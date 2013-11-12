/*
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
package org.fusesource.fabric.service;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.GetDataBuilder;
import org.junit.Test;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.fusesource.fabric.zookeeper.ZkPath.AUTHENTICATION_CRYPT_ALGORITHM;
import static org.fusesource.fabric.zookeeper.ZkPath.AUTHENTICATION_CRYPT_PASSWORD;
import static org.junit.Assert.assertEquals;

public class EncryptedPropertyResolverTest {


    @Test
    public void testResolve() throws Exception {
        CuratorFramework curator = createMock(CuratorFramework.class);
        GetDataBuilder getDataBuilder = createMock(GetDataBuilder.class);

        expect(curator.getData()).andReturn(getDataBuilder).anyTimes();
        expect(getDataBuilder.forPath(AUTHENTICATION_CRYPT_ALGORITHM.getPath())).andReturn("PBEWithMD5AndDES".getBytes()).anyTimes();
        expect(getDataBuilder.forPath(AUTHENTICATION_CRYPT_PASSWORD.getPath())).andReturn("mypassword".getBytes()).anyTimes();

        replay(curator);
        replay(getDataBuilder);
        EncryptedPropertyResolver resolver = new EncryptedPropertyResolver();
        resolver.bindCurator(curator);
        resolver.activate();
        assertEquals("encryptedpassword",resolver.resolve(null, null, null, "crypt:URdoo9++D3tsoC9ODrTfLNK5WzviknO3Ig6qbI2HuvQ="));
        verify(curator);
        verify(getDataBuilder);
    }
}

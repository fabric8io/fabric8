package org.fusesource.fabric.zookeeper.test;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.junit.Ignore;
import org.junit.Test;
import org.linkedin.zookeeper.client.IZKClient;

/**
 *
 */
public class MockIZKClientTest{

    @Ignore
    @Test
    public void testMockIZKClient() throws Exception {
        IZKClient c = MockIZKClient.newInstance();
        c.create("/foo/bar", "some data", ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
    }
}

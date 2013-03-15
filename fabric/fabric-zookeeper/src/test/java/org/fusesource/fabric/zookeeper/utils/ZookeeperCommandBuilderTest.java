package org.fusesource.fabric.zookeeper.utils;

import org.apache.zookeeper.KeeperException;
import org.fusesource.fabric.zookeeper.IZKClient;
import org.junit.Test;

import static org.easymock.EasyMock.*;

public class ZookeeperCommandBuilderTest {

    @Test
    public void testRetry() throws KeeperException, InterruptedException {
        IZKClient zookeeper = createMock(IZKClient.class);
        expect(zookeeper.getData("/tmp")).andThrow(new KeeperException.ConnectionLossException()).times(4);
        replay(zookeeper);
        try {
            ZookeeperCommandBuilder.getData("/tmp").execute(zookeeper);
        } catch (KeeperException ex) {
           ex.printStackTrace(System.out);
        }

        verify(zookeeper);
    }
}

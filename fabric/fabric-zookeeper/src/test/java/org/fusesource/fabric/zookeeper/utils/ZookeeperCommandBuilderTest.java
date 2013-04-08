package org.fusesource.fabric.zookeeper.utils;

import org.apache.zookeeper.KeeperException;
import org.fusesource.fabric.zookeeper.IZKClient;
import org.junit.Test;
import org.linkedin.util.clock.Timespan;

import java.util.concurrent.TimeoutException;

import static org.easymock.EasyMock.*;

public class ZookeeperCommandBuilderTest {

    @Test
    public void testRetry() throws KeeperException, InterruptedException, TimeoutException {
        IZKClient zookeeper = createMock(IZKClient.class);
        expect(zookeeper.getData("/tmp")).andThrow(new KeeperException.ConnectionLossException()).times(4);
        zookeeper.waitForConnected(eq(new Timespan(5, Timespan.TimeUnit.SECOND)));
        expectLastCall().anyTimes();
        replay(zookeeper);
        try {
            ZookeeperCommandBuilder.getData("/tmp").execute(zookeeper);
        } catch (KeeperException ex) {
           ex.printStackTrace(System.out);
        }

        verify(zookeeper);
    }
}

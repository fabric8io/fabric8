package org.fusesource.fabric.zookeeper.internal.locks;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.data.Stat;
import org.easymock.EasyMock;
import org.fusesource.fabric.zookeeper.IZKClient;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.easymock.EasyMock.eq;
import static org.junit.Assert.*;

public class LockImplTest {

    @Test
    public void testLockWithoutCompetition() throws KeeperException, InterruptedException {
        String id = "00000001";
        IZKClient zooKeeper = createMock(IZKClient.class);
        String path = "/my/lock";
        LockImpl lock = new LockImpl(zooKeeper, path);

        expect(zooKeeper.exists(path)).andReturn(new Stat()).once();
        expect(zooKeeper.create(ZkPath.LOCK.getPath(path), CreateMode.EPHEMERAL_SEQUENTIAL)).andReturn("/my/lock/" + id).once();
        expect(zooKeeper.getChildren(path)).andReturn(Arrays.asList(id)).once();
        replay(zooKeeper);
        assertTrue(lock.tryLock(30, TimeUnit.SECONDS));
        verify(zooKeeper);
    }

    @Test
    public void testLockWithCompetition() throws KeeperException, InterruptedException {
        String id = "00000001";
        String competitor = "00000002";
        IZKClient zooKeeper = createMock(IZKClient.class);
        String path = "/my/lock";
        LockImpl lock = new LockImpl(zooKeeper, path);

        expect(zooKeeper.exists(path)).andReturn(new Stat()).once();
        expect(zooKeeper.create(ZkPath.LOCK.getPath(path), CreateMode.EPHEMERAL_SEQUENTIAL)).andReturn("/my/lock/" + id).once();
        expect(zooKeeper.getChildren(path)).andReturn(Arrays.asList(id, competitor)).once();
        replay(zooKeeper);
        assertTrue(lock.tryLock(30, TimeUnit.SECONDS));
        verify(zooKeeper);
    }

    @Test
    public void testLockWithCompetitionAndLoose() throws KeeperException, InterruptedException {
        String id = "00000002";
        String competitor = "00000001";
        IZKClient zooKeeper = createMock(IZKClient.class);
        String path = "/my/lock";
        LockImpl lock = new LockImpl(zooKeeper, path);

        expect(zooKeeper.exists(path)).andReturn(new Stat()).once();
        expect(zooKeeper.create(ZkPath.LOCK.getPath(path), CreateMode.EPHEMERAL_SEQUENTIAL)).andReturn("/my/lock/" + id).once();
        expect(zooKeeper.getChildren(path)).andReturn(Arrays.asList(id, competitor)).anyTimes();
        expect(zooKeeper.exists(eq(path), EasyMock.<Watcher>anyObject())).andReturn(new Stat()).once();
        replay(zooKeeper);
        assertFalse(lock.tryLock(5, TimeUnit.SECONDS));
        verify(zooKeeper);
    }



    @Test
    public void testLockWithCompetitionAndLooseFirstThenWin() throws KeeperException, InterruptedException {
        String id = "00000002";
        String competitor = "00000001";
        IZKClient zooKeeper = createMock(IZKClient.class);
        String path = "/my/lock";
        final LockImpl lock = new LockImpl(zooKeeper, path);

        expect(zooKeeper.exists(path)).andReturn(new Stat()).once();
        expect(zooKeeper.create(ZkPath.LOCK.getPath(path), CreateMode.EPHEMERAL_SEQUENTIAL)).andReturn("/my/lock/" + id).once();
        expect(zooKeeper.getChildren(path)).andReturn(Arrays.asList(id, competitor)).once();
        expect(zooKeeper.exists(eq(path), EasyMock.<Watcher>anyObject())).andReturn(new Stat()).once();
        expect(zooKeeper.getChildren(path)).andReturn(Arrays.asList(id)).once();
        replay(zooKeeper);
        new Thread(new Runnable() {
            @Override
            public void run() {
                assertTrue(lock.tryLock(10, TimeUnit.SECONDS));
            }
        }).start();

        Thread.sleep(5000);
        synchronized (lock) {
            lock.notifyAll();
        }
        verify(zooKeeper);
    }
}

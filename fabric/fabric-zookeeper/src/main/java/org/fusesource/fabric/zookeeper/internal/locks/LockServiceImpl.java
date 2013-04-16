package org.fusesource.fabric.zookeeper.internal.locks;

import org.fusesource.fabric.zookeeper.IZKClient;
import org.fusesource.fabric.zookeeper.Lock;
import org.fusesource.fabric.zookeeper.LockService;

public class LockServiceImpl implements LockService {

    private IZKClient zooKeeper;

    @Override
    public Lock getLock(String path) {
        return new LockImpl(zooKeeper, path);
    }

    public IZKClient getZooKeeper() {
        return zooKeeper;
    }

    public void setZooKeeper(IZKClient zooKeeper) {
        this.zooKeeper = zooKeeper;
    }
}

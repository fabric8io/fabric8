package org.fusesource.fabric.zookeeper.internal.locks;

import org.fusesource.fabric.zookeeper.IZKClient;
import org.fusesource.fabric.zookeeper.Lock;
import org.fusesource.fabric.zookeeper.LockService;

public class LockServiceImpl implements LockService {

    @Override
    public Lock getLock(IZKClient zooKeeper, String path) {
        return new LockImpl(zooKeeper, path);
    }
}

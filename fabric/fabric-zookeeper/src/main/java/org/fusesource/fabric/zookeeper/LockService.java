package org.fusesource.fabric.zookeeper;

public interface LockService {

    Lock getLock(IZKClient zooKeeper, String path);
}

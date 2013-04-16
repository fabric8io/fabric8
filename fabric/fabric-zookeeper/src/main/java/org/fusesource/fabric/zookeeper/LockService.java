package org.fusesource.fabric.zookeeper;

public interface LockService {

    Lock getLock(String path);
}

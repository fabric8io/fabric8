package org.fusesource.fabric.zookeeper;

import java.util.concurrent.TimeUnit;

public interface Lock {

    public boolean tryLock(long time, TimeUnit unit);

    public void unlock();

}

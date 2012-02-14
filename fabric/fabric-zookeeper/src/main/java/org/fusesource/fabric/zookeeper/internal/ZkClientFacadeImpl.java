/**
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

package org.fusesource.fabric.zookeeper.internal;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;
import org.fusesource.fabric.zookeeper.ZkClientFacade;
import org.linkedin.zookeeper.client.IZKClient;
import org.linkedin.zookeeper.client.LifecycleListener;
import org.linkedin.zookeeper.client.ZKChildren;
import org.linkedin.zookeeper.client.ZKData;
import org.osgi.service.blueprint.container.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ZkClientFacadeImpl implements ZkClientFacade, ZooKeeperAware, LifecycleListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZkClientFacadeImpl.class);

    private IZKClient zooKeeper;
    private Long maximumWaitTime = 10000L;
    private Long retryInterval = 2000L;

    private String zookeeprUrl;

    final Object lock = new Object();


    public void onConnected() {
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    public void onDisconnected() {
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    public boolean isZooKeeperConfigured() {
        return zookeeprUrl != null && !zookeeprUrl.isEmpty();
    }

    @Override
    public boolean isZooKeeperConnected() {
        boolean connected = false;
        if (!isZooKeeperConfigured()) {
            return false;
        } else {
            try {
                connected = zooKeeper == null ? false : zooKeeper.isConnected();
            } catch (ServiceUnavailableException e) {
                connected = false;
            }
        }
        return connected;
    }

    /**
     * Checks that {@link IZKClient} is configured and connected.
     * If not it will throw an Exception.
     * @param connectTimeout  The amount of time to wait for client to connect.
     * @throws Exception
     */
    public void checkConnected(Long connectTimeout) throws Exception {
        if (!isZooKeeperConfigured()) {
            throw new Exception("Zookeeper client has not been configured yet. You need to either create an ensmelbe or join one.");
        }

        if (!isZooKeeperConnected()) {
            IZKClient client = getZookeeper(connectTimeout);
            if (client == null) {
              throw new Exception("Failed to connect to Zookeeper withing the specified timeout:" + connectTimeout);
            }
        }
    }

    /**
     * Waits for a valid Zookeeper client connection till the specified timeout.
     *
     * @param timeout
     * @return
     */
    public IZKClient getZookeeper(Long timeout) {
        if (!isZooKeeperConfigured()) {
            return null;
        }

        for (int t = 0; t <= timeout; t += retryInterval)   {
            try {

                if (zooKeeper != null && zooKeeper.isConnected()) {
                    return zooKeeper;
                }
            //TODO: This "catch" can be removed since its not used anymore. But first needs to be tested.
            } catch (ServiceUnavailableException ex) {
                LOGGER.warn("Zookeeper client service is not available yet. Retrying");
            }

            try {
                synchronized (lock) {
                    lock.wait(retryInterval);
                }
            } catch (InterruptedException e) {
                LOGGER.debug("Interrupted while waiting for the Zookeeper client service is not available yet.");
            }
        }
        throw new RuntimeException("Zookeeper client is not available yet.");
    }

    /**
     * Wait for a valid Zookeeper client connection for the maximumWaitTime.
     *
     * @return
     */
    public IZKClient getZookeeper() {
        return getZookeeper(maximumWaitTime);
    }

    public IZKClient getZooKeeper() {
        return zooKeeper;
    }

    public void setZooKeeper(IZKClient zooKeeper) {
        this.zooKeeper = zooKeeper;
    }

    @Override
    public void registerListener(LifecycleListener lifecycleListener) {
        getZookeeper().registerListener(lifecycleListener);
    }

    @Override
    public void removeListener(LifecycleListener lifecycleListener) {
        getZookeeper().removeListener(lifecycleListener);
    }

    @Override
    public IZKClient chroot(String s) {
        return getZookeeper().chroot(s);
    }

    @Override
    public boolean isConnected() {
        return getZookeeper().isConnected();
    }

    @Override
    public Stat exists(String s) throws InterruptedException, KeeperException {
        return getZookeeper().exists(s);
    }

    @Override
    public List<String> getChildren(String s) throws InterruptedException, KeeperException {
        return getZookeeper().getChildren(s);
    }

    @Override
    public ZKChildren getZKChildren(String s, Watcher watcher) throws KeeperException, InterruptedException {
        return getZookeeper().getZKChildren(s, watcher);
    }

    @Override
    public List<String> getAllChildren(String s) throws InterruptedException, KeeperException {
        return getZookeeper().getAllChildren(s);
    }

    @Override
    public void create(String s, String s1, List<ACL> acls, CreateMode createMode) throws InterruptedException, KeeperException {
        getZookeeper().create(s, s1, acls, createMode);
    }

    @Override
    public void createBytesNode(String s, byte[] bytes, List<ACL> acls, CreateMode createMode) throws InterruptedException, KeeperException {
        getZookeeper().createBytesNode(s, bytes, acls, createMode);
    }

    @Override
    public void createWithParents(String s, String s1, List<ACL> acls, CreateMode createMode) throws InterruptedException, KeeperException {
        getZookeeper().createWithParents(s, s1, acls, createMode);
    }

    @Override
    public void createBytesNodeWithParents(String s, byte[] bytes, List<ACL> acls, CreateMode createMode) throws InterruptedException, KeeperException {
        getZookeeper().createBytesNodeWithParents(s, bytes, acls, createMode);
    }

    @Override
    public byte[] getData(String s) throws InterruptedException, KeeperException {
        return getZookeeper().getData(s);
    }

    @Override
    public String getStringData(String s) throws InterruptedException, KeeperException {
        return getZookeeper().getStringData(s);
    }

    @Override
    public ZKData<String> getZKStringData(String s) throws InterruptedException, KeeperException {
        return getZookeeper().getZKStringData(s);
    }

    @Override
    public ZKData<String> getZKStringData(String s, Watcher watcher) throws InterruptedException, KeeperException {
        return getZookeeper().getZKStringData(s, watcher);
    }

    @Override
    public ZKData<byte[]> getZKByteData(String s) throws InterruptedException, KeeperException {
        return getZookeeper().getZKByteData(s);
    }

    @Override
    public ZKData<byte[]> getZKByteData(String s, Watcher watcher) throws InterruptedException, KeeperException {
        return getZookeeper().getZKByteData(s, watcher);
    }

    @Override
    public Stat setData(String s, String s1) throws InterruptedException, KeeperException {
        return getZookeeper().setData(s, s1);
    }

    @Override
    public Stat setByteData(String s, byte[] bytes) throws InterruptedException, KeeperException {
        return getZookeeper().setByteData(s, bytes);
    }

    @Override
    public Stat createOrSetWithParents(String s, String s1, List<ACL> acls, CreateMode createMode) throws InterruptedException, KeeperException {
        return getZookeeper().createOrSetWithParents(s, s1, acls, createMode);
    }

    @Override
    public void delete(String s) throws InterruptedException, KeeperException {
        getZookeeper().delete(s);
    }

    @Override
    public void deleteWithChildren(String s) throws InterruptedException, KeeperException {
        getZookeeper().deleteWithChildren(s);
    }

    @Override
    public String getConnectString() {
        return getZookeeper().getConnectString();
    }

    @Override
    public long getSessionId() {
        return getZookeeper().getSessionId();
    }

    @Override
    public byte[] getSessionPasswd() {
        return getZookeeper().getSessionPasswd();
    }

    @Override
    public int getSessionTimeout() {
        return getZookeeper().getSessionTimeout();
    }

    @Override
    public void addAuthInfo(String s, byte[] bytes) {
        getZookeeper().addAuthInfo(s, bytes);
    }

    @Override
    public void register(Watcher watcher) {
        getZookeeper().register(watcher);
    }

    @Override
    public void close() throws InterruptedException {
        getZookeeper().close();
    }

    @Override
    public String create(String s, byte[] bytes, List<ACL> acls, CreateMode createMode) throws KeeperException, InterruptedException {
        return getZookeeper().create(s, bytes, acls, createMode);
    }

    @Override
    public void create(String s, byte[] bytes, List<ACL> acls, CreateMode createMode, AsyncCallback.StringCallback stringCallback, Object o) {
        getZookeeper().create(s, bytes, acls, createMode, stringCallback, o);
    }

    @Override
    public void delete(String s, int i) throws InterruptedException, KeeperException {
        getZookeeper().delete(s, i);
    }

    @Override
    public void delete(String s, int i, AsyncCallback.VoidCallback voidCallback, Object o) {
        getZookeeper().delete(s, i, voidCallback, o);
    }

    @Override
    public Stat exists(String s, Watcher watcher) throws KeeperException, InterruptedException {
        return getZookeeper().exists(s, watcher);
    }

    @Override
    public Stat exists(String s, boolean b) throws KeeperException, InterruptedException {
        return getZookeeper().exists(s, b);
    }

    @Override
    public void exists(String s, Watcher watcher, AsyncCallback.StatCallback statCallback, Object o) {
        getZookeeper().exists(s, watcher, statCallback, o);
    }

    @Override
    public void exists(String s, boolean b, AsyncCallback.StatCallback statCallback, Object o) {
        getZookeeper().exists(s, b, statCallback, o);
    }

    @Override
    public byte[] getData(String s, Watcher watcher, Stat stat) throws KeeperException, InterruptedException {
        return getZookeeper().getData(s, watcher, stat);
    }

    @Override
    public byte[] getData(String s, boolean b, Stat stat) throws KeeperException, InterruptedException {
        return getZookeeper().getData(s, b, stat);
    }

    @Override
    public void getData(String s, Watcher watcher, AsyncCallback.DataCallback dataCallback, Object o) {
        getZookeeper().getData(s, watcher, dataCallback, o);
    }

    @Override
    public void getData(String s, boolean b, AsyncCallback.DataCallback dataCallback, Object o) {
        getZookeeper().getData(s, b, dataCallback, o);
    }

    @Override
    public Stat setData(String s, byte[] bytes, int i) throws KeeperException, InterruptedException {
        return getZookeeper().setData(s, bytes, i);
    }

    @Override
    public void setData(String s, byte[] bytes, int i, AsyncCallback.StatCallback statCallback, Object o) {
        getZookeeper().setData(s, bytes, i, statCallback, o);
    }

    @Override
    public List<ACL> getACL(String s, Stat stat) throws KeeperException, InterruptedException {
        return getZookeeper().getACL(s, stat);
    }

    @Override
    public void getACL(String s, Stat stat, AsyncCallback.ACLCallback aclCallback, Object o) {
        getZookeeper().getACL(s, stat, aclCallback, o);
    }

    @Override
    public Stat setACL(String s, List<ACL> acls, int i) throws KeeperException, InterruptedException {
        return getZookeeper().setACL(s, acls, i);
    }

    @Override
    public void setACL(String s, List<ACL> acls, int i, AsyncCallback.StatCallback statCallback, Object o) {
        getZookeeper().setACL(s, acls, i, statCallback, o);
    }

    @Override
    public List<String> getChildren(String s, Watcher watcher) throws KeeperException, InterruptedException {
        return getZookeeper().getChildren(s, watcher);
    }

    @Override
    public List<String> getChildren(String s, boolean b) throws KeeperException, InterruptedException {
        return getZookeeper().getChildren(s, b);
    }

    @Override
    public void getChildren(String s, Watcher watcher, AsyncCallback.ChildrenCallback childrenCallback, Object o) {
        getZookeeper().getChildren(s, watcher, childrenCallback, o);
    }

    @Override
    public void getChildren(String s, boolean b, AsyncCallback.ChildrenCallback childrenCallback, Object o) {
        getZookeeper().getChildren(s, b, childrenCallback, o);
    }

    @Override
    public List<String> getChildren(String s, Watcher watcher, Stat stat) throws KeeperException, InterruptedException {
        return getZookeeper().getChildren(s, watcher, stat);
    }

    @Override
    public List<String> getChildren(String s, boolean b, Stat stat) throws KeeperException, InterruptedException {
        return getZookeeper().getChildren(s, b, stat);
    }

    @Override
    public void getChildren(String s, Watcher watcher, AsyncCallback.Children2Callback children2Callback, Object o) {
        getZookeeper().getChildren(s, watcher, children2Callback, o);
    }

    @Override
    public void getChildren(String s, boolean b, AsyncCallback.Children2Callback children2Callback, Object o) {
        getZookeeper().getChildren(s, b, children2Callback, o);
    }

    @Override
    public void sync(String s, AsyncCallback.VoidCallback voidCallback, Object o) {
        getZookeeper().sync(s, voidCallback, o);
    }

    @Override
    public ZooKeeper.States getState() {
        return getZookeeper().getState();
    }

    public Long getMaximumWaitTime() {
        return maximumWaitTime;
    }

    public void setMaximumWaitTime(Long maximumWaitTime) {
        this.maximumWaitTime = maximumWaitTime;
    }

    public long getRetryInterval() {
        return retryInterval;
    }

    public void setRetryInterval(long retryInterval) {
        this.retryInterval = retryInterval;
    }

    public String getZookeeprUrl() {
        return zookeeprUrl;
    }

    public void setZookeeprUrl(String zookeeprUrl) {
        this.zookeeprUrl = zookeeprUrl;
    }
}

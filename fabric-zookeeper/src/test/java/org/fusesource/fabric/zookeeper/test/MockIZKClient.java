package org.fusesource.fabric.zookeeper.test;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;
import org.linkedin.zookeeper.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

/**
 *
 */
public class MockIZKClient implements IZKClient {

    protected static final Logger LOG = LoggerFactory.getLogger(MockIZKClient.class);

    private IZKClient self;

    public static IZKClient newInstance() {
        return createProxy(new MockIZKClient());
    }

    private static IZKClient createProxy(MockIZKClient target) {
        final MockIZKClient tmp = target;
        return (IZKClient)Proxy.newProxyInstance(
                target.getClass().getClassLoader(),
                target.getClass().getInterfaces(),
                new InvocationHandler() {
                    MockIZKClient obj = tmp;
                    @Override
                    public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
                        String callee = String.format("%s.%s", obj.getClass().getSimpleName(), method);
                        LOG.trace(String.format("%s(%s)", callee, objects));
                        Object rc = null;
                        try {
                            rc = method.invoke(obj, objects);
                            LOG.trace(String.format("%s returning %s", callee, rc));
                            return rc;
                        } catch (Throwable t) {
                            LOG.trace(String.format("%s throwing %s", callee, t));
                            throw t;
                        }
                    }
                });
    }

    private MockIZKClient() {
        self = createProxy(this);
    }

    @Override
    public void registerListener(LifecycleListener listener) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeListener(LifecycleListener listener) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public IZKClient chroot(String path) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isConnected() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Stat exists(String path) throws InterruptedException, KeeperException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<String> getChildren(String path) throws InterruptedException, KeeperException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<String> getAllChildren(String path) throws InterruptedException, KeeperException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public byte[] getData(String path) throws InterruptedException, KeeperException {
        return new byte[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getStringData(String path) throws InterruptedException, KeeperException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ZKData<String> getZKStringData(String path) throws InterruptedException, KeeperException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ZKData<byte[]> getZKByteData(String path) throws InterruptedException, KeeperException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Stat setData(String path, String data) throws InterruptedException, KeeperException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Stat setByteData(String path, byte[] data) throws InterruptedException, KeeperException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void delete(String path) throws InterruptedException, KeeperException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void deleteWithChildren(String path) throws InterruptedException, KeeperException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Stat createOrSetWithParents(String path, String data, List<ACL> acl, CreateMode createMode) throws InterruptedException, KeeperException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ZKData<byte[]> getZKByteData(String path, Watcher watcher) throws InterruptedException, KeeperException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ZKData<String> getZKStringData(String path, Watcher watcher) throws InterruptedException, KeeperException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void createBytesNodeWithParents(String path, byte[] data, List<ACL> acl, CreateMode createMode) throws InterruptedException, KeeperException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void createWithParents(String path, String data, List<ACL> acl, CreateMode createMode) throws InterruptedException, KeeperException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void createBytesNode(String path, byte[] data, List<ACL> acl, CreateMode createMode) throws InterruptedException, KeeperException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void create(String path, String data, List<ACL> acl, CreateMode createMode) throws InterruptedException, KeeperException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ZKChildren getZKChildren(String path, Watcher watcher) throws KeeperException, InterruptedException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public long getSessionId() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public byte[] getSessionPasswd() {
        return new byte[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getSessionTimeout() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addAuthInfo(String scheme, byte[] auth) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void close() throws InterruptedException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void delete(String path, int version) throws InterruptedException, KeeperException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Stat exists(String path, boolean watch) throws KeeperException, InterruptedException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Stat setData(String path, byte[] data, int version) throws KeeperException, InterruptedException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<String> getChildren(String path, boolean watch) throws KeeperException, InterruptedException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ZooKeeper.States getState() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void sync(String path, AsyncCallback.VoidCallback cb, Object ctx) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void getChildren(String path, boolean watch, AsyncCallback.Children2Callback cb, Object ctx) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void getChildren(String path, Watcher watcher, AsyncCallback.Children2Callback cb, Object ctx) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<String> getChildren(String path, boolean watch, Stat stat) throws KeeperException, InterruptedException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<String> getChildren(String path, Watcher watcher, Stat stat) throws KeeperException, InterruptedException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void getChildren(String path, boolean watch, AsyncCallback.ChildrenCallback cb, Object ctx) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void getChildren(String path, Watcher watcher, AsyncCallback.ChildrenCallback cb, Object ctx) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<String> getChildren(String path, Watcher watcher) throws KeeperException, InterruptedException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setACL(String path, List<ACL> acl, int version, AsyncCallback.StatCallback cb, Object ctx) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Stat setACL(String path, List<ACL> acl, int version) throws KeeperException, InterruptedException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void getACL(String path, Stat stat, AsyncCallback.ACLCallback cb, Object ctx) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<ACL> getACL(String path, Stat stat) throws KeeperException, InterruptedException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setData(String path, byte[] data, int version, AsyncCallback.StatCallback cb, Object ctx) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void getData(String path, boolean watch, AsyncCallback.DataCallback cb, Object ctx) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void getData(String path, Watcher watcher, AsyncCallback.DataCallback cb, Object ctx) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public byte[] getData(String path, boolean watch, Stat stat) throws KeeperException, InterruptedException {
        return new byte[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public byte[] getData(String path, Watcher watcher, Stat stat) throws KeeperException, InterruptedException {
        return new byte[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void exists(String path, boolean watch, AsyncCallback.StatCallback cb, Object ctx) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void exists(String path, Watcher watcher, AsyncCallback.StatCallback cb, Object ctx) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Stat exists(String path, Watcher watcher) throws KeeperException, InterruptedException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void delete(String path, int version, AsyncCallback.VoidCallback cb, Object ctx) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void create(String path, byte[] data, List<ACL> acl, CreateMode createMode, AsyncCallback.StringCallback cb, Object ctx) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String create(String path, byte[] data, List<ACL> acl, CreateMode createMode) throws KeeperException, InterruptedException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void register(Watcher watcher) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}

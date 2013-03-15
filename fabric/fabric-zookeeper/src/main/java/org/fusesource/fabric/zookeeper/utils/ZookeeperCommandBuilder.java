package org.fusesource.fabric.zookeeper.utils;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.proto.WatcherEvent;
import org.fusesource.fabric.zookeeper.IZKClient;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.fusesource.fabric.zookeeper.internal.OsgiZkClient;
import org.linkedin.util.clock.Timespan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class ZookeeperCommandBuilder<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperCommandBuilder.class);

    private final ZookeeperCommand<T> command;
    private int retries = 3;
    private long retryDelay = 5000L;
    private long timeout = 60000L;

    private ZookeeperCommandBuilder(ZookeeperCommand<T> commmnd) {
        this.command = commmnd;
    }

    public static ZookeeperCommandBuilder<Stat> exists(String znode) {
        return new ZookeeperCommandBuilder<Stat>(new ZookeeperExistsommand(znode));
    }

    public static ZookeeperCommandBuilder<Stat> set(String znode, String data) {
        return new ZookeeperCommandBuilder<Stat>(new ZokeeperSetStringCommand(znode, data));
    }

    public static ZookeeperCommandBuilder<Stat> set(String znode, byte[] data) {
        return new ZookeeperCommandBuilder<Stat>(new ZokeeperSetBytesCommand(znode, data));
    }

    public static ZookeeperCommandBuilder<String> create(String znode) {
        return new ZookeeperCommandBuilder<String>(new ZookeeperCreateCommand(znode));
    }

    public static ZookeeperCommandBuilder<Boolean> delete(String znode) {
        return new ZookeeperCommandBuilder<Boolean>(new ZookeeperDeleteCommand(znode));
    }

    public static ZookeeperCommandBuilder<byte[]> loadUrl(String znode) {
        return new ZookeeperCommandBuilder<byte[]>(new ZookeeperLoadUrlCommand(znode));
    }

    public static ZookeeperCommandBuilder<byte[]> getData(String znode) {
        return new ZookeeperCommandBuilder<byte[]>(new ZookeeperGetBytesCommand(znode));
    }

    public static ZookeeperCommandBuilder<String> getStringData(String znode) {
        return new ZookeeperCommandBuilder<String>(new ZookeeperGetStrinngCommand(znode));
    }

    public static ZookeeperCommandBuilder<List<String>> getChildren(String znode) {
        return new ZookeeperCommandBuilder<List<String>>(new ZookeeperGetChildrenCommand(znode));
    }

    public static ZookeeperCommandBuilder<Boolean> fixAcls(String znode, Boolean recursive) {
        return new ZookeeperCommandBuilder<Boolean>(new ZookeeperFixAclsCommand(znode, recursive));
    }

    public ZookeeperCommandBuilder withRetries(int retries) {
        this.retries = retries;
        return this;
    }

    public ZookeeperCommandBuilder withRetryDelay(long retryDelay) {
        this.retries = retries;
        return this;
    }


    public T execute(IZKClient zooKeeper) throws KeeperException, InterruptedException {
        boolean rethrow = false;
        long startTime = System.currentTimeMillis();
        KeeperException lastThrown = null;
        for (int r = 0; r <= retries && (System.currentTimeMillis() - startTime < timeout); r++) {
            if (r == retries) {
                rethrow = true;
            }
            try {
                return command.execute(zooKeeper);
            } catch (KeeperException ex) {
                lastThrown = ex;
                if (rethrow || !shouldRetry(ex.code().intValue())) {
                    throw ex;
                } else {
                    LOGGER.warn("Caught recoverable zookeeper exception {}. Retrying {}.", ex.code().name(), r + 1);
                    waitForZookeeper(zooKeeper);
                }
            } catch (IllegalStateException ex) {
                LOGGER.warn("Caught illegal state exception on zookeeper. Retrying {}.", r + 1);
                lastThrown = new KeeperException.OperationTimeoutException();
                waitForZookeeper(zooKeeper);
            }
        }
        LOGGER.warn("Exhausted retry attempts for recoverable Zookeeper error. Rethrowing.");
        throw lastThrown;
    }

    private void waitForZookeeper(IZKClient zooKeeper) {
        try {
            zooKeeper.waitForConnected(Timespan.milliseconds(retryDelay));
        } catch (Exception e) {
            //noop
        }
    }

    public static boolean shouldRetry(int rc) {
        return (rc == KeeperException.Code.CONNECTIONLOSS.intValue()) ||
                (rc == KeeperException.Code.OPERATIONTIMEOUT.intValue()) ||
                (rc == KeeperException.Code.SESSIONMOVED.intValue()) ||
                (rc == KeeperException.Code.SESSIONEXPIRED.intValue());
    }


    private interface ZookeeperCommand<T> {

        T execute(IZKClient zooKeeper) throws KeeperException, InterruptedException, IllegalStateException;
    }

    private static class ZookeeperExistsommand implements ZookeeperCommand<Stat> {
        private final String znode;

        ZookeeperExistsommand(String znode) {
            this.znode = znode;
        }

        @Override
        public Stat execute(IZKClient zooKeeper) throws KeeperException, InterruptedException {
            return zooKeeper.exists(znode);
        }
    }

    private static class ZokeeperSetStringCommand implements ZookeeperCommand<Stat> {

        private final String znode;
        private final String data;

        ZokeeperSetStringCommand(String znode, String data) {
            this.znode = znode;
            this.data = data;
        }

        @Override
        public Stat execute(IZKClient zooKeeper) throws KeeperException, InterruptedException {
            return zooKeeper.createOrSetWithParents(znode, data, CreateMode.PERSISTENT);
        }
    }

    private static class ZokeeperSetBytesCommand implements ZookeeperCommand<Stat> {

        private final String znode;
        private final byte[] data;

        ZokeeperSetBytesCommand(String znode, byte[] data) {
            this.znode = znode;
            this.data = data;
        }

        @Override
        public Stat execute(IZKClient zooKeeper) throws KeeperException, InterruptedException {
            return zooKeeper.createOrSetWithParents(znode, data, CreateMode.PERSISTENT);
        }
    }

    private static class ZookeeperCreateCommand implements ZookeeperCommand<String> {
        private final String znode;

        ZookeeperCreateCommand(String znode) {
            this.znode = znode;
        }

        @Override
        public String execute(IZKClient zooKeeper) throws KeeperException, InterruptedException {
            try {
                return zooKeeper.createWithParents(znode, CreateMode.PERSISTENT);
            } catch (KeeperException.NodeExistsException ex) {
                //Ignore
            }
            return znode;
        }
    }

    private static class ZookeeperDeleteCommand implements ZookeeperCommand<Boolean> {
        private final String znode;

        ZookeeperDeleteCommand(String znode) {
            this.znode = znode;
        }

        @Override
        public Boolean execute(IZKClient zooKeeper) throws KeeperException, InterruptedException {
            try {
                zooKeeper.deleteWithChildren(znode);
                return true;
            } catch (KeeperException.NoNodeException ex) {
                return false;
            }
        }
    }

    private static class ZookeeperGetStrinngCommand implements ZookeeperCommand<String> {
        private final String znode;

        ZookeeperGetStrinngCommand(String znode) {
            this.znode = znode;
        }

        @Override
        public String execute(IZKClient zooKeeper) throws KeeperException, InterruptedException {
            return zooKeeper.getStringData(znode);
        }
    }


    private static class ZookeeperGetBytesCommand implements ZookeeperCommand<byte[]> {
        private final String znode;

        ZookeeperGetBytesCommand(String znode) {
            this.znode = znode;
        }

        @Override
        public byte[] execute(IZKClient zooKeeper) throws KeeperException, InterruptedException {
            return zooKeeper.getData(znode);
        }
    }


    private static class ZookeeperLoadUrlCommand implements ZookeeperCommand<byte[]> {
        private final String path;

        ZookeeperLoadUrlCommand(String path) {
            this.path = path;
        }

        @Override
        public byte[] execute(IZKClient zooKeeper) throws KeeperException, InterruptedException {
            try {
                return ZkPath.loadURL(zooKeeper, path);
            } catch (IOException e) {
                return new byte[0];
            } catch (URISyntaxException e) {
                return new byte[0];
            }
        }
    }

    private static class ZookeeperGetChildrenCommand implements ZookeeperCommand<List<String>> {
        private final String znode;

        ZookeeperGetChildrenCommand(String znode) {
            this.znode = znode;
        }

        @Override
        public List<String> execute(IZKClient zooKeeper) throws KeeperException, InterruptedException {
            return zooKeeper.getChildren(znode);
        }
    }

    private static class ZookeeperFixAclsCommand implements ZookeeperCommand<Boolean> {
        private final String znode;
        private final Boolean recursive;

        ZookeeperFixAclsCommand(String znode, Boolean recursive) {
            this.znode = znode;
            this.recursive = recursive;
        }

        @Override
        public Boolean execute(IZKClient zooKeeper) throws KeeperException, InterruptedException {
            zooKeeper.fixACLs(znode, recursive);
            return true;
        }
    }

}



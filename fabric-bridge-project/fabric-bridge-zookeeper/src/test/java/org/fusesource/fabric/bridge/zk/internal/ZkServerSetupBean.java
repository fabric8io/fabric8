package org.fusesource.fabric.bridge.zk.internal;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.service.FabricServiceImpl;
import org.linkedin.zookeeper.client.IZKClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.SmartLifecycle;

/**
 * @author Dhiraj Bokde
 */
public class ZkServerSetupBean implements SmartLifecycle {

    private FabricService fabricService;
    private volatile boolean running;
    private static final Logger LOG = LoggerFactory.getLogger(ZkServerSetupBean.class);

    public FabricService getFabricService() {
        return fabricService;
    }

    public void setFabricService(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    /**
     * Setup vanilla ZK server.
     */
    @Override
    public void start() {
        IZKClient client = ((FabricServiceImpl)fabricService).getZooKeeper();

        // import ZK contents
        TestImport testImport = new TestImport();
        testImport.setSource("src/test/resources/zkexport");
        testImport.setNRegEx(new String[] {"dummy"});
        testImport.setZooKeeper(client);
        try {
            testImport.doExecute();
        } catch (Exception e) {
            String msg = "Error setting up ZK config: " + e.getMessage();
            LOG.error(msg, e);
            throw new BeanCreationException(msg, e);
        }
        running = true;
    }

    @Override
    public void stop() {
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return 1;
    }

}

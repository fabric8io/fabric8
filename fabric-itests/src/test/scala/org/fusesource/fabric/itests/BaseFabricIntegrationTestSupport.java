package org.fusesource.fabric.itests;

import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class BaseFabricIntegrationTestSupport {

    protected Logger LOG = LoggerFactory.getLogger(this.getClass().getName());

    private EmbeddedFabricService server = null;

    public EmbeddedFabricService getServer() {
        return server;
    }

    @Before
    public void initialize() throws Exception {
        server = new EmbeddedFabricService();
        server.start();
    }

    @After
    public void cleanup() throws Exception {
        server.stop();
    }

}

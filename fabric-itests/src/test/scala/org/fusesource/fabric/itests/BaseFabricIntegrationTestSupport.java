package org.fusesource.fabric.itests;

import org.junit.After;
import org.junit.Before;

/**
 *
 */
public class BaseFabricIntegrationTestSupport {

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

    /*
@Test
public void testFabricServiceImpl() throws Exception {
    Logger log = LoggerFactory.getLogger("test");
    BundleContext bc = registry.getBundleContext();

    for (Bundle b : bc.getBundles()) {
        log.info(String.format("Found bundle : %s with version %s in state %s", b.getSymbolicName(), b.getVersion(), b.getState()));
    }

    for (ServiceReference ref : bc.getAllServiceReferences(null, null)) {
        log.info(String.format("Found Service reference : %s", ref.toString()));
    }
        FabricService service = getServer().getFabricService();

        log.info("Profiles :");
        for (Profile profile : service.getDefaultVersion().getProfiles()) {
            log.info("Profile : " + profile.getId());
        }

    }
    */
}

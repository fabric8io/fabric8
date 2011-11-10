package org.fusesource.fabric.itests;

import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.Profile;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertTrue;

/**
 *
 */
public class FabricServiceTest extends BaseFabricIntegrationTestSupport {

    protected FabricService service;

    @Before
    public void setFabricService() throws Exception {
        service = getServer().getFabricService();
    }

    @Test
    public void testCreateProfile() throws Exception {

        Profile[] profiles = service.getDefaultVersion().getProfiles();

        Profile test = service.getDefaultVersion().createProfile("testCreateProfile");

        profiles = service.getDefaultVersion().getProfiles();

        assertTrue(Arrays.asList(profiles).contains(test));
    }

}

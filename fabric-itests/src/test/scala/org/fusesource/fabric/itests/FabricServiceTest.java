package org.fusesource.fabric.itests;

import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.Profile;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertTrue;

/**
 *
 */
public class FabricServiceTest extends BaseFabricIntegrationTestSupport {

    @Test
    public void testCreateProfile() throws Exception {
        FabricService service = getServer().getFabricService();

        Profile[] profiles = service.getDefaultVersion().getProfiles();

        Profile test = service.getDefaultVersion().createProfile("testCreateProfile");

        profiles = service.getDefaultVersion().getProfiles();

        assertTrue(Arrays.asList(profiles).contains(test));
    }

}

package io.fabric8.agent.download;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import io.fabric8.agent.download.impl.MavenDownloadManager;
import io.fabric8.agent.utils.AgentUtils;
import io.fabric8.api.Constants;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.Profiles;
import io.fabric8.maven.MavenResolver;
import io.fabric8.maven.MavenResolvers;

public final class DownloadManagers {

    /**
     * Creates a download manager using the current container's maven configuration
     */
    public static DownloadManager createDownloadManager(FabricService fabricService, ScheduledExecutorService executorService) {
        Profile overlayProfile = fabricService.getCurrentContainer().getOverlayProfile();
        Profile effectiveProfile = Profiles.getEffectiveProfile(fabricService, overlayProfile);
        return createDownloadManager(fabricService, effectiveProfile, executorService);
    }

    /**
     * Creates a DownloadManager
     */
    public static DownloadManager createDownloadManager(FabricService fabricService, Profile profile, ScheduledExecutorService executorService) {
        Map<String, String> configuration = profile.getConfiguration(Constants.AGENT_PID);
        if (configuration == null) {
            configuration = new HashMap<>();
        }
        Dictionary<String, String> properties = mapToDictionary(configuration);
        AgentUtils.addMavenProxies(properties, fabricService);
        MavenResolver resolver = MavenResolvers.createMavenResolver(properties, "org.ops4j.pax.url.mvn");
        return createDownloadManager(resolver, executorService);
    }

    /**
     * Creates a DownloadManager
     */
    public static DownloadManager createDownloadManager(MavenResolver resolver, ScheduledExecutorService executorService) {
        return new MavenDownloadManager(resolver, executorService);
    }

    /**
     * Utility method for converting a {@link java.util.Map} into {@link java.util.Properties}
     */
    private static Dictionary<String, String> mapToDictionary(Map<String, String> map) {
        Hashtable<String, String> p = new Hashtable<>();
        Set<Map.Entry<String, String>> set = map.entrySet();
        for (Map.Entry<String, String> entry : set) {
            p.put(entry.getKey(), entry.getValue());
        }
        return p;
    }

    // Private constructor
    private DownloadManagers() { }
}

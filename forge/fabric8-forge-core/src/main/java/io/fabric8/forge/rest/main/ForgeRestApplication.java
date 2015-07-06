package io.fabric8.forge.rest.main;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import io.fabric8.forge.rest.RootResource;
import org.apache.cxf.feature.LoggingFeature;
import io.fabric8.forge.rest.CommandsResource;
import io.fabric8.forge.rest.RepositoriesResource;

import javax.inject.Inject;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@ApplicationPath("/")
public class ForgeRestApplication extends Application {
    @Inject
    ForgeInitialiser forgeInitialiser;

    @Inject
    RootResource rootResource;

    @Inject
    CommandsResource commandsResource;

    @Inject
    RepositoriesResource repositoriesResource;

    private boolean preloaded = false;

    @Override
    public Set<Object> getSingletons() {
        if (!preloaded) {
            preloaded = true;
            forgeInitialiser.preloadCommands(commandsResource);
        }

        return new HashSet<Object>(
                Arrays.asList(
                        rootResource,
                        commandsResource,
                        repositoriesResource,
                        new JacksonJsonProvider(),
/*
                        new SwaggerFeature(),
                        new EnableJMXFeature(),
*/
                        new LoggingFeature()
                )
        );
    }
}

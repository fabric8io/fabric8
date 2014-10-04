package io.fabric8.insight.elasticsearch.discovery;

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.discovery.Discovery;

public class FabricDiscoveryModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(Discovery.class).to(FabricDiscovery.class).asEagerSingleton();
    }
}
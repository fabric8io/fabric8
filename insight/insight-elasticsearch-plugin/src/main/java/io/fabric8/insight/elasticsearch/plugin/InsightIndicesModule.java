package io.fabric8.insight.elasticsearch.plugin;

import org.elasticsearch.common.inject.AbstractModule;

public class InsightIndicesModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(InsightIndicesHousekeeperService.class).asEagerSingleton();
    }

}

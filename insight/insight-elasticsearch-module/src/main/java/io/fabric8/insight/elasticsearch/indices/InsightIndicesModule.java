package io.fabric8.insight.elasticsearch.indices;

import org.elasticsearch.common.inject.AbstractModule;

public class InsightIndicesModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(InsightIndicesHousekeeperService.class).asEagerSingleton();
    }

}

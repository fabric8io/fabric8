package io.fabric8.insight.elasticsearch.indices;

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.settings.Settings;

public class IndicesManagementModule extends AbstractModule {

    private final Settings settings;

    public IndicesManagementModule(Settings settings) {
        this.settings = settings;
    }

    @Override
    protected void configure() {
        bind(IndicesManagementService.class).asEagerSingleton();
    }

}

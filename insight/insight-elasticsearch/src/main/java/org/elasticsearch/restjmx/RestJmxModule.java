package org.elasticsearch.restjmx;

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.http.HttpServerManagement;

public class RestJmxModule extends AbstractModule {

    private final Settings settings;

    public RestJmxModule(Settings settings) {
        this.settings = settings;
    }

    @Override
    protected void configure() {
        bind(RestJmxManagement.class).asEagerSingleton();
    }

}

package io.fabric8.insight.elasticsearch.discovery;

import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.AbstractPlugin;

import java.util.Collection;

public class FabricDiscoveryPlugin extends AbstractPlugin {

    private final Settings settings;

    public FabricDiscoveryPlugin(Settings settings) {
        this.settings = settings;
    }

    @Override
    public String name() {
        return "fabric8-discovery";
    }

    @Override
    public String description() {
        return "Discovery module using Fabric8";
    }
}

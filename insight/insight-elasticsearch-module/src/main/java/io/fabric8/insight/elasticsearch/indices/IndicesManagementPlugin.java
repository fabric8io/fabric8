package io.fabric8.insight.elasticsearch.indices;

import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.AbstractPlugin;

import java.util.Collection;

public class IndicesManagementPlugin extends AbstractPlugin {

    private final Settings settings;

    public IndicesManagementPlugin(Settings settings) {
        this.settings = settings;
    }

    @Override
    public String name() {
        return "insight-indices-management";
    }

    @Override
    public String description() {
        return "Manages Insight indices, performing configurable housekeeping of data";
    }

    @Override
    public Collection<Class<? extends Module>> modules() {
        Collection<Class<? extends Module>> modules = Lists.newArrayList();
        modules.add(InsightIndicesModule.class);
        return modules;
    }
}

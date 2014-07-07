package io.fabric8.insight.elasticsearch.indices;

import java.util.Collection;

import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.component.LifecycleComponent;
import org.elasticsearch.plugins.AbstractPlugin;

public class IndicesManagementPlugin extends AbstractPlugin {

    @Override
    public String name() {
        return "indices-management";
    }

    @Override
    public String description() {
        return "Automatic management of per-day indices";
    }

//    @Override
//    public Collection<Class<? extends Module>> modules() {
//        Collection<Class<? extends Module>> modules = Lists.newArrayList();
//        modules.add(IndicesManagementModule.class);
//        return modules;
//    }

    @Override
    public Collection<Class<? extends LifecycleComponent>> services() {
        Collection<Class<? extends LifecycleComponent>> services = Lists.newArrayList();
        services.add(IndicesManagementService.class);
        return services;
    }
}

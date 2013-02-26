package org.elasticsearch.restjmx;

import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.AbstractPlugin;

import java.util.Collection;
import java.util.Collections;

public class RestJmxPlugin extends AbstractPlugin {

    @Override
    public String name() {
        return "restjmx";
    }

    @Override
    public String description() {
        return "RestJmx support";
    }

    @Override
    public Collection<Module> modules(Settings settings) {
        return Collections.<Module>singleton(new RestJmxModule(settings));
    }

}

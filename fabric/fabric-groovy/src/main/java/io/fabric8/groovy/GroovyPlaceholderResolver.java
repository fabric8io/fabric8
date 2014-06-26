/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.groovy;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import io.fabric8.api.FabricException;
import io.fabric8.api.FabricService;
import io.fabric8.api.PlaceholderResolver;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.zookeeper.utils.ZooKeeperFacade;
import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@ThreadSafe
@Component(name = "io.fabric8.placholder.resolver.groovy", label = "Fabric8 Groovy Placeholder Resolver", metatype = false)
@Service({ PlaceholderResolver.class, GroovyPlaceholderResolver.class })
@Properties({ @Property(name = "scheme", value = GroovyPlaceholderResolver.RESOLVER_SCHEME) })
public final class GroovyPlaceholderResolver extends AbstractComponent implements PlaceholderResolver {

    public static final String RESOLVER_SCHEME = "groovy";

    private static final Logger LOGGER = LoggerFactory.getLogger(GroovyPlaceholderResolver.class);

    @Activate
    void activate() {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    @Override
    public String getScheme() {
        return RESOLVER_SCHEME;
    }

    @Override
    public String resolve(FabricService fabricService, Map<String, Map<String, String>> configs, String pid, String key, String value) {
        CuratorFramework curator = fabricService.adapt(CuratorFramework.class);
        return resolveValue(curator, value);
    }

    /**
     * Returns the replacement of groovy expressions on the given string value.
     */
    public static String resolveValue(CuratorFramework curator, String value) {
        try {
            Binding binding = new Binding();
            ZooKeeperFacade zk = new ZooKeeperFacade(curator);
            binding.setVariable("zk", zk);
            GroovyShell shell = new GroovyShell(binding);
            String expression = value;
            if (expression.startsWith(RESOLVER_SCHEME + ":")) {
                expression = expression.substring(RESOLVER_SCHEME.length() + 1);
            }
            Object result = shell.evaluate(expression);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("groovy expression: " + expression + " => " + result);
            }
            if (result != null) {
                return result.toString();
            }
            return "";
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }
}

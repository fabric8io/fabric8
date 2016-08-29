/**
 * Copyright 2016 Red Hat, Inc.
 * <p/>
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.fabric8.karaf.blueprint;

import java.util.Dictionary;
import java.util.concurrent.atomic.AtomicReference;

import io.fabric8.karaf.core.properties.PlaceholderResolver;
import org.apache.aries.blueprint.ext.evaluator.PropertyEvaluator;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;

/**
 * Ports Camel's env: sys: service: service.host: service.port: property
 * placeholder prefix resolution strategies so that they are supported in Blueprint
 * via an evaluator.
 *
 * see: http://camel.apache.org/using-propertyplaceholder.html
 *
 * It supports chained evaluators i.e ${env+service:MY_ENV_VAR} where the first
 * step is to resolve MY_ENV_VAR against environment variables then the result is
 * resolved using service function.
 */
@Component(
    immediate = true,
    policy = ConfigurationPolicy.IGNORE,
    createPid = false)
@Properties({
    @Property(name = "org.apache.aries.blueprint.ext.evaluator.name", value = "fabric8")
})
@Reference(
    name  = "resolver",
    cardinality = ReferenceCardinality.MANDATORY_UNARY,
    policy = ReferencePolicy.STATIC,
    referenceInterface  = PlaceholderResolver.class
)
@Service(PropertyEvaluator.class)
public class Fabric8PropertyEvaluator implements PropertyEvaluator {
    private final AtomicReference<PlaceholderResolver> resolver;

    public Fabric8PropertyEvaluator() {
        resolver = new AtomicReference<>();
    }

    @Override
    public String evaluate(String key, Dictionary<String, String> dictionary) {
        PlaceholderResolver res = resolver.get();
        String value = null;

        if (res != null) {
            value = res.resolve(key);
        }

        return value != null ? value : dictionary.get(key);
    }

    // ****************************
    // Binding
    // ****************************

    protected void bindResolver(PlaceholderResolver resolver) {
        this.resolver.set(resolver);
    }

    protected void unbindResolver(PlaceholderResolver resolver) {
        this.resolver.compareAndSet(resolver, null);
    }
}

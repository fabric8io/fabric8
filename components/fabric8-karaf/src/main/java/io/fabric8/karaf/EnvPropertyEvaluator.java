/**
 *  Copyright 2016 Red Hat, Inc.
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

package io.fabric8.karaf;

import org.apache.aries.blueprint.ext.evaluator.PropertyEvaluator;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;

import java.util.Dictionary;


@Component(
    name = "io.fabric8.karaf.env",
    label="Environment Property Evaluator",
    immediate = true,
    enabled = true,
    policy = ConfigurationPolicy.IGNORE,
    createPid = false,
    description = "A PropertyEvaluator which resolves env: prefixed properties against OS environment variables"
)
@org.apache.felix.scr.annotations.Properties({ @Property(
    name = "org.apache.aries.blueprint.ext.evaluator.name", value = "env")
})
@Service(PropertyEvaluator.class)
public class EnvPropertyEvaluator implements PropertyEvaluator {

    @Override
    public String evaluate(String name, Dictionary<String, String> dictionary) {
        if( name.startsWith("env:") ) {
            String envVar = name.substring(4);
            return System.getenv(envVar);
        }
        return dictionary.get(name);
    }

}

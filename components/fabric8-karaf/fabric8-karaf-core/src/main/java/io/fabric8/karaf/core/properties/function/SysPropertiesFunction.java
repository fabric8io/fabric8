/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.karaf.core.properties.function;

import io.fabric8.karaf.core.Support;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;

/**
 * A {@link PropertiesFunction} that lookup the property value from
 * JVM system property.
 */
@Component(
    immediate = true,
    policy = ConfigurationPolicy.IGNORE,
    createPid = false
)
@Property(name = "function.name", value = SysPropertiesFunction.FUNCTION_NAME)
@Service(PropertiesFunction.class)
public class SysPropertiesFunction implements PropertiesFunction {
    public static final String FUNCTION_NAME = "sys";

    @Override
    public String getName() {
        return FUNCTION_NAME;
    }

    @Override
    public String apply(String remainder) {
        String key = remainder;
        String defaultValue = null;

        if (remainder.contains(":")) {
            key = Support.before(remainder, ":");
            defaultValue = Support.after(remainder, ":");
        }

        String value = System.getProperty(key);
        return value != null ? value : defaultValue;
    }

}

/*
 * #%L
 * Gravia :: Runtime :: API
 * %%
 * Copyright (C) 2013 - 2014 JBoss by Red Hat
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.fabric8.api.gravia;


/**
 * An properties provider.
 *
 * @author thomas.diesler@jboss.com
 * @since 02-Jun-2013
 * 
 * @ThreadSafe
 */
public abstract class AbstractPropertiesProvider implements PropertiesProvider {

    @Override
    public Object getProperty(String key) {
        return getProperty(key, null);
    }

    @Override
	public Object getRequiredProperty(String key) {
        Object value = getProperty(key, null);
        IllegalStateAssertion.assertNotNull(value, "Cannot obtain property: " + key);
		return value;
	}
}

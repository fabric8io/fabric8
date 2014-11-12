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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A {@link PropertiesProvider} backed by a {@link java.util.Map}.
 */
public class MapPropertiesProvider extends AbstractPropertiesProvider {

	private final Map<String, Object> properties = new ConcurrentHashMap<String, Object>();

	public MapPropertiesProvider() {
		this(new HashMap<String, Object>());
	}

	public MapPropertiesProvider(Properties props) {
		this(propsToMap(props));
	}

	public MapPropertiesProvider(Map<String, Object> props) {
		IllegalArgumentAssertion.assertNotNull(props, "props");
		properties.putAll(props);
	}

	@Override
	public Object getProperty(String key, Object defaultValue) {
		Object value = properties.get(key);
		return value != null ? value : defaultValue;
	}

	private static Map<String, Object> propsToMap(Properties props) {
		Map<String, Object> result = new HashMap<String, Object>();
		synchronized (props) {
			for (Map.Entry<Object, Object> entry : props.entrySet()) {
				String key = entry.getKey().toString();
				Object value = entry.getValue();
				result.put(key, value);
			}
		}
		return result;
	}
}

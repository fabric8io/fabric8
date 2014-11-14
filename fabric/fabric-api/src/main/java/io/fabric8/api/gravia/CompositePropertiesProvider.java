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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A Composite {@link PropertiesProvider}.
 */
public class CompositePropertiesProvider extends AbstractPropertiesProvider {

	private final List<PropertiesProvider> delegates;

	public CompositePropertiesProvider(PropertiesProvider... delegates) {
		IllegalArgumentAssertion.assertNotNull(delegates, "delegates");
		this.delegates = Arrays.asList(delegates);
	}

	@Override
	public Object getProperty(String key, Object defaultValue) {
		Object result = null;
		for (PropertiesProvider delegate : delegates) {
			result = delegate.getProperty(key);
			if (result != null) {
				return result;
			}
		}
		return defaultValue;
	}

    List<PropertiesProvider> getDelegates() {
        return Collections.unmodifiableList(delegates);
    }
}

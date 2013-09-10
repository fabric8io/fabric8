/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.fabric.service;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.fusesource.fabric.api.PlaceholderResolver;

@Component(name = "org.fusesource.fabric.placholder.resolver.env",
        description = "Environment Placeholder Resolver")
@Service(PlaceholderResolver.class)
public class EnvPlaceholderResolver implements PlaceholderResolver {

    private static final String ENV_SCHEME = "env";
    @Override
    public String getScheme() {
        return ENV_SCHEME;
    }

    @Override
    public String resolve(String pid, String key, String value) {
        if (value != null && value.length() > ENV_SCHEME.length())  {
            String name = value.substring(ENV_SCHEME.length() + 1);
            return System.getenv(name);
        }
        return value;
    }
}

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
package io.fabric8.api;

import java.util.Map;

public interface PlaceholderResolver {

    /**
     * The placeholder scheme.
     */
    public String getScheme();

    /**
     * Resolves the placeholder found inside the value, for the specific key of the pid.
     * @param fabricService The fabric service
     * @param pid       The pid that contains the placeholder.
     * @param key       The key of the configuration value that contains the placeholder.
     * @param value     The value with the placeholder.
     * @return          The resolved value or EMPTY_STRING.
     */
    public String resolve(FabricService fabricService, Map<String, Map<String, String>> configs, String pid, String key, String value);

}

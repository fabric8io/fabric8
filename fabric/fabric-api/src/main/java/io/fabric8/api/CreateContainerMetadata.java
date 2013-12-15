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

import java.io.Serializable;
import java.util.Map;

public interface CreateContainerMetadata<O extends CreateContainerOptions> extends Serializable {

    boolean isSuccess();

    Throwable getFailure();

    Container getContainer();
    void setContainer(Container container);

    String getContainerName();

    Map<String, String> getContainerConfiguration();

    O getCreateOptions();

    void setCreateOptions(CreateContainerOptions options);

    String getOverridenResolver();

    /**
     * Updates the value of {@link CreateContainerOptions} with updated credentials.
     * @param user
     * @param credential
     */
    void updateCredentials(String user, String credential);
}

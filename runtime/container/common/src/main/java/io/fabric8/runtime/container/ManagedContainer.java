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
package io.fabric8.runtime.container;

import java.io.File;
import java.util.Iterator;
import java.util.ServiceLoader;

import org.jboss.gravia.runtime.RuntimeType;


/**
 * The managed root container
 *
 * @since 26-Feb-2014
 */
public interface ManagedContainer<T extends ContainerConfiguration> {

    enum State {
        CREATED,
        STARTED,
        STOPPED,
        DESTROYED
    }

    RuntimeType getRuntimeType();

    File getContainerHome();

    State getState();

    void create(T configuration) throws LifecycleException;

    void start() throws LifecycleException;

    void stop() throws LifecycleException;

    void destroy() throws LifecycleException;

    public static class Factory<T extends ContainerConfiguration> {

        @SuppressWarnings({ "rawtypes", "unchecked" })
        public static <T extends ContainerConfiguration> ManagedContainer<T> create(T configuration) throws LifecycleException {
            RuntimeType type = configuration.getRuntimeType();
            ServiceLoader<ManagedContainer> loader = ServiceLoader.load(ManagedContainer.class);
            Iterator<ManagedContainer> iterator = loader.iterator();
            while(iterator.hasNext()) {
                ManagedContainer<T> service = iterator.next();
                if (service.getRuntimeType() == type) {
                    service.create(configuration);
                    return service;
                }
            }
            throw new IllegalStateException("Cannot obtain managed container service for: " + type);
        }
    }
}

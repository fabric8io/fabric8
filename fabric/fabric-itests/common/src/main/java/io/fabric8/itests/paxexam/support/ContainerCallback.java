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
package io.fabric8.itests.paxexam.support;

import io.fabric8.api.Container;
import org.fusesource.tooling.testing.pax.exam.karaf.FuseTestSupport;

public enum ContainerCallback implements Callback<Container> {

    DISPLAY_ALL {
        @Override
        public void call(Container container) {
            LIST_BUNDLES.call(container);
            LIST_COMPONENTS.call(container);
            THREAD_DUMP.call(container);
            DISPLAY_EXCEPTION.call(container);
        }
    },

    LIST_BUNDLES {
        @Override
        public void call(Container container) {
            System.err.println(FuseTestSupport.executeCommand("fabric:container-connect -u admin -p admin " + container.getId() + " osgi:list -t 0"));
        }
    },

    LIST_COMPONENTS {
        @Override
        public void call(Container container) {
            System.err.println(FuseTestSupport.executeCommand("fabric:container-connect -u admin -p admin " + container.getId() + " scr:list"));
        }
    },

    THREAD_DUMP {
        @Override
        public void call(Container container) {
            System.err.println(FuseTestSupport.executeCommand("fabric:container-connect -u admin -p admin " + container.getId() + " threads --dump"));
        }
    },

    DISPLAY_EXCEPTION {
        @Override
        public void call(Container container) {
            System.err.println(FuseTestSupport.executeCommand("fabric:container-connect -u admin -p admin " + container.getId() + " log:display-exception"));
        }
    },

    DO_NOTHING {
        @Override
        public void call(Container container) {
        }
    }

}

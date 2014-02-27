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
    }

}

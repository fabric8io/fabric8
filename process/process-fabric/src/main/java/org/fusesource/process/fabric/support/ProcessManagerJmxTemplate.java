package org.fusesource.process.fabric.support;

import io.fabric8.api.Container;
import io.fabric8.service.ContainerTemplate;
import io.fabric8.service.JmxTemplateSupport;
import org.fusesource.process.fabric.ProcessManagerCallback;
import org.fusesource.process.manager.service.ProcessManagerServiceMBean;

import javax.management.remote.JMXConnector;

public class ProcessManagerJmxTemplate extends ContainerTemplate {

    public ProcessManagerJmxTemplate(Container container, String login, String password, boolean cacheJmx) {
        super(container, login, password, cacheJmx);
    }

    public <T> T execute(final ProcessManagerCallback<T> callback) {
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            return getJmxTemplate().execute(new JmxTemplateSupport.JmxConnectorCallback<T>() {
                public T doWithJmxConnector(JMXConnector connector) throws Exception {
                    String[] bean = new String[]{"type", "LocalProcesses"};
                    return callback.doWithProcessManager(getJmxTemplate().getMBean(connector, ProcessManagerServiceMBean.class, "io.fabric8", bean));
                }
            });
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }

}

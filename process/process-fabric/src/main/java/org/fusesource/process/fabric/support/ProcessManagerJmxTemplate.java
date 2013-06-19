package org.fusesource.process.fabric.support;

import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.service.ContainerTemplate;
import org.fusesource.fabric.service.JmxTemplateSupport;
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
                    return callback.doWithProcessManager(getJmxTemplate().getMBean(connector, ProcessManagerServiceMBean.class, "org.fusesource.fabric", bean));
                }
            });
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }

}

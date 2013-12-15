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
package io.fabric8.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.karaf.admin.management.AdminServiceMBean;
import io.fabric8.api.Container;
import io.fabric8.api.FabricAuthenticationException;
import io.fabric8.api.FabricException;
import org.fusesource.insight.log.service.LogQueryCallback;
import org.osgi.jmx.framework.BundleStateMBean;
import org.osgi.jmx.framework.ServiceStateMBean;

/**
 * A utitily class for interacting with a remote Container via JMX
 */
public class ContainerTemplate {
    private final Container container;
    private final JmxTemplateSupport jmxTemplate;
    protected String login;
    protected String password;

    private ContainerTemplate(Container container, JmxTemplateSupport jmxTemplate) {
        this.jmxTemplate = jmxTemplate;
        this.container = container;
    }

    private ContainerTemplate(Container container, boolean cacheJmx) {
        this.container = container;
        if (cacheJmx) {
            this.jmxTemplate = new ContainerCachingJmxTemplate(this);
        } else {
            this.jmxTemplate = new NonCachingJmxTemplate() {
                @Override
                protected JMXConnector createConnector() {
                    return ContainerTemplate.this.createConnector();
                }
            };
        }
    }

    public ContainerTemplate(Container container, String login, String password, boolean cacheJmx) {
        this(container, cacheJmx);
        this.login = login;
        this.password = password;
    }


    public interface AdminServiceCallback<T> {

        T doWithAdminService(AdminServiceMBean adminService) throws Exception;

    }

    public interface BundleStateCallback<T> {

        T doWithBundleState(BundleStateMBean bundleState) throws Exception;
    }

    public interface ServiceStateCallback<T> {

        T doWithServiceState(ServiceStateMBean serviceState) throws Exception;
    }

    public <T> T execute(JmxTemplateSupport.JmxConnectorCallback<T> callback) {
        return jmxTemplate.execute(callback);
    }

    public <T> T execute(final LogQueryCallback<T> callback) {
        return jmxTemplate.execute(callback);
    }

    // TODO we could refactor all these execute() methods to work at the JmxTemplate level and just delegate to them
    // then folks could use these APIs using a JmxTemplate only

    public <T> T execute(final AdminServiceCallback<T> callback) {
        return jmxTemplate.execute(new JmxTemplateSupport.JmxConnectorCallback<T>() {
            public T doWithJmxConnector(JMXConnector connector) throws Exception {
                String[] bean = new String[]{"type", "admin", "name", container.getId()};
                return callback.doWithAdminService(jmxTemplate.getMBean(connector, AdminServiceMBean.class, "org.apache.karaf", bean));
            }
        });
    }

    public <T> T execute(final BundleStateCallback<T> callback) {
        return jmxTemplate.execute(new JmxTemplateSupport.JmxConnectorCallback<T>() {
            public T doWithJmxConnector(JMXConnector connector) throws Exception {
                String[] bean = new String[] {"type", "bundleState", "version", "1.5"};
                return callback.doWithBundleState(jmxTemplate.getMBean(connector, BundleStateMBean.class, "osgi.core", bean));
            }
        });
    }

    public <T> T execute(final ServiceStateCallback<T> callback) {
        return jmxTemplate.execute(new JmxTemplateSupport.JmxConnectorCallback<T>() {
            public T doWithJmxConnector(JMXConnector connector) throws Exception {
                String[] bean = new String[]{"type", "serviceState", "version", "1.5"};
                return callback.doWithServiceState(jmxTemplate.getMBean(connector, ServiceStateMBean.class, "osgi.core", bean));
            }
        });
    }


    public static Map getEnvCred(String login, String password) {
        Map env = new HashMap<String, Object>();
        env.put(JMXConnector.CREDENTIALS, new String[] {login, password});
        return env;
    }

    public Map getEnvironmentCredentials() {
        return getEnvCred(login, password);
    }

    public Container getContainer() {
        return container;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public JmxTemplateSupport getJmxTemplate() {
        return jmxTemplate;
    }

    public JMXConnector createConnector() {
        String rootUrl = container.getJmxUrl();
        if (rootUrl == null) {
            throw new IllegalStateException("The jmx url for container '" + container.getId() + "' is not specified");
        }
        JMXConnector connector;
        try {
            connector = JMXConnectorFactory.connect(
                    new JMXServiceURL(rootUrl),
                    getEnvironmentCredentials());
        } catch (IOException e) {
            throw FabricException.launderThrowable(e);
        } catch (SecurityException ex) {
            throw new FabricAuthenticationException(ex);
        }
        return connector;
    }
}

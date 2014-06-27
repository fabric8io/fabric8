/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package org.wildfly.extension.fabric.service;

import io.fabric8.api.RuntimeProperties;
import io.fabric8.api.ZooKeeperClusterBootstrap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.gravia.resource.Attachable;
import org.jboss.gravia.runtime.Module;
import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.runtime.ServiceEvent;
import org.jboss.gravia.runtime.ServiceListener;
import org.jboss.gravia.runtime.ServiceReference;
import org.jboss.gravia.runtime.spi.AbstractModule;
import org.jboss.gravia.runtime.spi.ClassLoaderEntriesProvider;
import org.jboss.gravia.runtime.spi.ManifestHeadersProvider;
import org.jboss.gravia.runtime.spi.ModuleEntriesProvider;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.extension.fabric.FabricConstants;
import org.wildfly.extension.gravia.GraviaConstants;

/**
 * Service responsible for creating and managing the life-cycle of the gravia subsystem.
 *
 * @since 19-Apr-2013
 */
public class FabricBootstrapService extends AbstractService<ZooKeeperClusterBootstrap> {

    static final Logger LOGGER = LoggerFactory.getLogger(FabricConstants.class.getPackage().getName());

    private final InjectedValue<ModuleContext> injectedModuleContext = new InjectedValue<ModuleContext>();
    private final InjectedValue<Runtime> injectedRuntime = new InjectedValue<Runtime>();

    private ZooKeeperClusterBootstrap bootstrapService;
    private Module module;

    public ServiceController<ZooKeeperClusterBootstrap> install(ServiceTarget serviceTarget, ServiceVerificationHandler verificationHandler) {
        ServiceBuilder<ZooKeeperClusterBootstrap> builder = serviceTarget.addService(FabricConstants.FABRIC_SUBSYSTEM_SERVICE_NAME, this);
        builder.addDependency(GraviaConstants.MODULE_CONTEXT_SERVICE_NAME, ModuleContext.class, injectedModuleContext);
        builder.addDependency(GraviaConstants.RUNTIME_SERVICE_NAME, Runtime.class, injectedRuntime);
        builder.addListener(verificationHandler);
        return builder.install();
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        LOGGER.info("Activating Fabric Subsystem");

        // Initialize ConfigurationAdmin content
        Runtime runtime = injectedRuntime.getValue();
        initConfigurationAdmin(runtime);

        // Start listening on the {@link ZooKeeperClusterBootstrap}
        final CountDownLatch latch = new CountDownLatch(1);
        final ModuleContext syscontext = injectedModuleContext.getValue();
        ServiceListener listener = new ServiceListener() {
            @Override
            public void serviceChanged(ServiceEvent event) {
                if (event.getType() == ServiceEvent.REGISTERED) {
                    ServiceReference<?> sref = event.getServiceReference();
                    bootstrapService = (ZooKeeperClusterBootstrap) syscontext.getService(sref);
                    syscontext.removeServiceListener(this);
                    latch.countDown();
                }
            }
        };
        syscontext.addServiceListener(listener, "(objectClass=" + ZooKeeperClusterBootstrap.class.getName() + ")");

        // Install and start this as a {@link Module}
        ModuleClassLoader classLoader = (ModuleClassLoader) getClass().getClassLoader();
        try {
            URL url = classLoader.getResource(JarFile.MANIFEST_NAME);
            Manifest manifest = new Manifest(url.openStream());
            Dictionary<String, String> headers = new ManifestHeadersProvider(manifest).getHeaders();
            module = runtime.installModule(classLoader, headers);

            // Attach the {@link ModuleEntriesProvider} so
            ModuleEntriesProvider entriesProvider = new ClassLoaderEntriesProvider(module);
            Attachable attachable = AbstractModule.assertAbstractModule(module);
            attachable.putAttachment(AbstractModule.MODULE_ENTRIES_PROVIDER_KEY, entriesProvider);

            // Start the module
            module.start();

        } catch (RuntimeException rte) {
            throw rte;
        } catch (Exception ex) {
            throw new StartException(ex);
        }

        // Wait for the {@link ZooKeeperClusterBootstrap} to come up
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new StartException("Cannot obtain ZooKeeperClusterBootstrap");
            }
        } catch (InterruptedException ex) {
            // ignore
        }

        // FuseFabric banner message
        Properties brandingProperties = new Properties();
        String resname = "/META-INF/branding.properties";
        try {
            URL brandingURL = getClass().getResource(resname);
            brandingProperties.load(brandingURL.openStream());
        } catch (IOException e) {
            throw new StartException("Cannot read branding properties from: " + resname);
        }
        System.out.println(brandingProperties.getProperty("welcome"));
    }

    @Override
    public void stop(StopContext context) {
        // Uninstall the bootstrap module
        if (module != null) {
            module.uninstall();
        }
    }

    @Override
    public ZooKeeperClusterBootstrap getValue() throws IllegalStateException {
        return bootstrapService;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void initConfigurationAdmin(Runtime runtime) {
        ModuleContext syscontext = runtime.getModuleContext();
        ConfigurationAdmin configAdmin = syscontext.getService(syscontext.getServiceReference(ConfigurationAdmin.class));
        File confDir = new File((String) runtime.getProperty(RuntimeProperties.RUNTIME_CONF_DIR));
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".cfg");
            }
        };
        for (String name : confDir.list(filter)) {
            String pid = name.substring(0, name.length() - 4);
            try {
                FileInputStream fis = new FileInputStream(new File(confDir, name));
                Properties props = new Properties();
                props.load(fis);
                fis.close();

                Configuration config = configAdmin.getConfiguration(pid, null);
                config.update((Hashtable) props);
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }
}

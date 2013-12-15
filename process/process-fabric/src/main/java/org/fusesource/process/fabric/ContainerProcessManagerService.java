package org.fusesource.process.fabric;

import com.google.common.collect.ImmutableMap;
import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import org.fusesource.process.fabric.support.ProcessManagerJmxTemplate;
import org.fusesource.process.manager.InstallTask;
import org.fusesource.process.manager.Installation;
import org.fusesource.process.manager.service.ProcessManagerServiceMBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.util.List;

public class ContainerProcessManagerService implements ContainerProcessManagerServiceMBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerProcessManagerService.class);

    private final ObjectName objectName;
    private FabricService fabricService;
    private MBeanServer mbeanServer;

    public ContainerProcessManagerService() throws MalformedObjectNameException {
        this.objectName = new ObjectName("io.fabric8:type=FabricProcesses");
    }

    public void bindMBeanServer(MBeanServer mbeanServer) {
        unbindMBeanServer(this.mbeanServer);
        this.mbeanServer = mbeanServer;
        if (mbeanServer != null) {
            registerMBeanServer(mbeanServer);
        }
    }

    public void unbindMBeanServer(MBeanServer mbeanServer) {
        if (mbeanServer != null) {
            unregisterMBeanServer(mbeanServer);
            this.mbeanServer = null;
        }
    }

    public void registerMBeanServer(MBeanServer mbeanServer) {
        try {
            if (!mbeanServer.isRegistered(objectName)) {
                mbeanServer.registerMBean(this, objectName);
            }
        } catch (Exception e) {
            LOGGER.warn("An error occured during mbean server registration: " + e, e);
        }
    }

    public void unregisterMBeanServer(MBeanServer mbeanServer) {
        if (mbeanServer != null) {
            try {
                ObjectName name = objectName;
                if (mbeanServer.isRegistered(name)) {
                    mbeanServer.unregisterMBean(name);
                }
            } catch (Exception e) {
                LOGGER.warn("An error occured during mbean server registration: " + e, e);
            }
        }
    }

    private ProcessManagerJmxTemplate getJmxTemplate(Container container, String jmxUser, String jmxPassword) {
        return new ProcessManagerJmxTemplate(container, jmxUser, jmxPassword, true);
    }

    public FabricService getFabricService() {
        return fabricService;
    }

    public void setFabricService(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    @Override
    public List<Installation> listInstallations(final ContainerInstallOptions options) {
        Container container = fabricService.getContainer(options.getContainer());
        ProcessManagerJmxTemplate jmxTemplate = getJmxTemplate(container, options.getUser(), options.getPassword());
        return jmxTemplate.execute(new ProcessManagerCallback<List<Installation>>() {
            @Override
            public List<Installation> doWithProcessManager(ProcessManagerServiceMBean processManagerService) throws Exception {
                return processManagerService.listInstallations();
            }
        });
    }

    @Override
    public Installation install(final ContainerInstallOptions options, final InstallTask postInstall) throws Exception {
        Container container = fabricService.getContainer(options.getContainer());
        ProcessManagerJmxTemplate jmxTemplate = getJmxTemplate(container, options.getUser(), options.getPassword());
        return jmxTemplate.execute(new ProcessManagerCallback<Installation>() {
            @Override
            public Installation doWithProcessManager(ProcessManagerServiceMBean processManagerService) throws Exception {
                return processManagerService.install(options.asInstallOptions(), postInstall);
            }
        });
    }

    @Override
    public Installation installJar(final ContainerInstallOptions options) throws Exception {
        Container container = fabricService.getContainer(options.getContainer());
        ProcessManagerJmxTemplate jmxTemplate = getJmxTemplate(container, options.getUser(), options.getPassword());
        return jmxTemplate.execute(new ProcessManagerCallback<Installation>() {
            @Override
            public Installation doWithProcessManager(ProcessManagerServiceMBean processManagerService) throws Exception {
                return processManagerService.installJar(options.asInstallOptions());
            }
        });
    }

    @Override
    public ImmutableMap<Integer, Installation> listInstallationMap(final ContainerInstallOptions options) {
        Container container = fabricService.getContainer(options.getContainer());
        ProcessManagerJmxTemplate jmxTemplate = getJmxTemplate(container, options.getUser(), options.getPassword());
        return jmxTemplate.execute(new ProcessManagerCallback<ImmutableMap<Integer, Installation>>() {
            @Override
            public ImmutableMap<Integer, Installation> doWithProcessManager(ProcessManagerServiceMBean processManagerService) throws Exception {
                return processManagerService.listInstallationMap();
            }
        });
    }
}

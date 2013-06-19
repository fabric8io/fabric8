package org.fusesource.process.fabric;

import com.google.common.collect.ImmutableMap;
import org.fusesource.process.manager.InstallTask;
import org.fusesource.process.manager.Installation;

import java.util.List;

public interface ContainerProcessManager {

    /**
     * Returns the current installed processes which may or may not be running right now
     */
    List<Installation> listInstallations(ContainerInstallOptions options);

    /**
     * Installs a process with an optional URL of the controller JSON
     * together with an optional install step
     */
    Installation install(ContainerInstallOptions options, InstallTask postInstall) throws Exception;

    /**
     * Installs an executable jar as a new managed process
     */
    Installation installJar(ContainerInstallOptions options) throws Exception;

    ImmutableMap<Integer, Installation> listInstallationMap(ContainerInstallOptions options);
}

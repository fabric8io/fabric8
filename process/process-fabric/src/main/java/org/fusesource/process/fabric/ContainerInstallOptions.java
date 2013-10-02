package org.fusesource.process.fabric;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import org.fusesource.process.manager.InstallOptions;

public class ContainerInstallOptions extends InstallOptions {

    public static class ContainerInstallOptionsBuilder extends InstallOptionsBuilder<ContainerInstallOptionsBuilder> {

        private String container;
        private String user;
        private String password;

        public ContainerInstallOptionsBuilder container(final String container) {
            this.container = container;
            return this;
        }

        public ContainerInstallOptionsBuilder user(final String user) {
            this.user = user;
            return this;
        }

        public ContainerInstallOptionsBuilder password(final String password) {
            this.password = password;
            return this;
        }

        public ContainerInstallOptions build() throws MalformedURLException {
                return new ContainerInstallOptions(container, user, password, getName(), getUrl(), getControllerUrl(), getExtractCmd(), isOffline(), getOptionalDependencyPatterns(), getExcludeDependencyFilterPatterns(), getMainClass(), getProperties());

        }
    }

    private final String container;
    private final String user;
    private final String password;

    public static ContainerInstallOptionsBuilder builder() {
        return new ContainerInstallOptionsBuilder();
    }

    public ContainerInstallOptions(String container, String user, String password, String name, URL url, URL controllerUrl, String extractCmd, boolean offline, String[] optionalDependencyPatterns, String[] excludeDependencyFilterPatterns, String mainClass, Map<String, Object> properties) {
        super(name, url, controllerUrl, extractCmd, offline, optionalDependencyPatterns, excludeDependencyFilterPatterns, mainClass, properties);
        this.container = container;
        this.user = user;
        this.password = password;
    }

    public String getContainer() {
        return container;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public InstallOptions asInstallOptions() {
        return new InstallOptions(getName(), getUrl(), getControllerUrl(), getExtractCmd(), isOffline(), getOptionalDependencyPatterns(), getExcludeDependencyFilterPatterns(), getMainClass(), getProperties());
    }
}

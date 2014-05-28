package io.fabric8.container.process;

import io.fabric8.api.Container;
import io.fabric8.api.CreateChildContainerMetadata;
import io.fabric8.api.CreateChildContainerOptions;
import io.fabric8.api.FabricService;
import io.fabric8.api.scr.Configurer;
import io.fabric8.process.manager.InstallOptions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class ProcessManagerControllerTest extends Assert {

    // Collaborators fixtures

    Configurer configurer = mock(Configurer.class);

    FabricService fabricService = mock(FabricService.class);

    Container container = mock(Container.class);

    CreateChildContainerOptions containerOptions = new CreateChildContainerOptions.Builder().build();

    CreateChildContainerMetadata containerMetadata = new CreateChildContainerMetadata();

    JavaContainerConfig javaContainerConfig = new JavaContainerConfig();

    // Test subject fixture

    ProcessManagerController managerController = new ProcessManagerController(null, configurer, null, fabricService, null) {
        @Override
        protected JavaContainerConfig createJavaContainerConfig() {
            javaContainerConfig.setMainClass("com.main.Class");
            return javaContainerConfig;
        }

        @Override
        protected Map<String, File> extractJarsFromProfiles(Container container, CreateChildContainerOptions installOptions) throws Exception {
            return new HashMap<String, File>();
        }
    };

    // Fixtures setup

    @Before
    public void before() {
        System.setProperty("java.protocol.handler.pkgs", "org.ops4j.pax.url");

        given(fabricService.getEnvironment()).willReturn("env");
    }

    // Tests

    @Test
    public void shouldCopyJarUrlFromJavaContainerConfigToInstallOptions() throws Exception {
        // Given
        String mainJar = "mvn:org.apache.camel/camel-xstream/2.12.0";
        javaContainerConfig.setJarUrl(mainJar);

        // When
        InstallOptions installOptions = managerController.createJavaInstallOptions(container, containerMetadata, containerOptions, new HashMap<String, String>());

        // Then
        assertEquals(new URL(mainJar), installOptions.getUrl());
    }

    @Test
    public void shouldIgnoreNullJarUrlInJavaContainerConfig() throws Exception {
        // When
        InstallOptions installOptions = managerController.createJavaInstallOptions(container, containerMetadata, containerOptions, new HashMap<String, String>());

        // Then
        assertNull(installOptions.getUrl());
    }

    @Test
    public void shouldCopyJvmOptionsFromJavaContainerConfigToInstallOptions() throws Exception {
        // Given
        String firstVmOption = "-Dfoo=bar";
        String secondVmOption = "-Dbaz=qux";
        javaContainerConfig.setJvmArguments(firstVmOption + " " + secondVmOption);

        // When
        InstallOptions installOptions = managerController.createJavaInstallOptions(container, containerMetadata, containerOptions, new HashMap<String, String>());

        // Then
        assertArrayEquals(new String[]{firstVmOption, secondVmOption}, installOptions.getJvmOptions());
    }

}

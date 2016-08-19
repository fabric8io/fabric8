/**
 * Copyright (C) 2015 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.karaf.itests;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import javax.inject.Inject;

import org.apache.karaf.features.FeaturesService;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureSecurity;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFileExtend;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class FeatureInstallationTest extends TestBase {

    @Inject
    FeaturesService featuresService;

    @Test
    public void testFabric8KarafBlueprint() throws Exception {
        featuresService.addRepository(getFeaturesFile().toURI());
        featuresService.installFeature("fabric8-karaf-blueprint");
    }

    @Ignore
    @Test
    public void testFabric8KarafCm() throws Exception {
        featuresService.addRepository(getFeaturesFile().toURI());
        featuresService.installFeature("fabric8-karaf-cm");
    }

    @Test
    public void testFabric8KarafChecks() throws Exception {
        featuresService.addRepository(getFeaturesFile().toURI());
        featuresService.installFeature("fabric8-karaf-checks");
    }

    @Configuration
    public Option[] config() throws URISyntaxException, MalformedURLException {
        return new Option[] {
            karafDistributionConfiguration()
                .frameworkUrl(getKarafMinimalUrl())
                .name("Apache Karaf")
                .unpackDirectory(new File("target/exam")),
            configureSecurity()
                .disableKarafMBeanServerBuilder(),
            keepRuntimeFolder(),
            editConfigurationFilePut(
                "etc/system.properties", 
                "features.xml", 
                System.getProperty("features.xml")),
            editConfigurationFileExtend(
                "etc/org.ops4j.pax.url.mvn.cfg",
                "org.ops4j.pax.url.mvn.repositories",
                "file:"+System.getProperty("features.repo")+"@id=local@snapshots@releases"),
            systemProperty("kubernetes.namespace").value("my-namespace"),
            systemProperty("kubernetes.master").value("http://my.kube.master:8443"),
            systemProperty("kubernetes.auth.tryKubeConfig").value("false"),
            logLevel(LogLevelOption.LogLevel.INFO),
        };
    }
}
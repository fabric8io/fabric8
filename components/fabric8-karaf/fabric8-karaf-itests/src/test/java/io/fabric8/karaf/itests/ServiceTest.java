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

import io.fabric8.karaf.blueprint.Fabric8PropertyEvaluator;
import io.fabric8.karaf.cm.PlaceholderResolverConfigurationPlugin;
import io.fabric8.karaf.core.properties.PlaceholderResolver;
import io.fabric8.karaf.core.properties.PlaceholderResolverImpl;
import org.apache.aries.blueprint.ext.evaluator.PropertyEvaluator;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.util.Filter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationPlugin;

import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureSecurity;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFileExtend;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ServiceTest extends TestBase {

    @Inject
    protected BundleContext bundleContext;

    @Inject
    @Filter("(component.name=io.fabric8.karaf.core.properties.PlaceholderResolverImpl)")
    PlaceholderResolver placeholderResolver;

    @Inject
    @Filter("(component.name=io.fabric8.karaf.blueprint.Fabric8PropertyEvaluator)")
    PropertyEvaluator propertyEvaluator;

    @Inject
    @Filter("(component.name=io.fabric8.karaf.cm.PlaceholderResolverConfigurationPlugin)")
    ConfigurationPlugin configurationPlugin;

    @Test
    public void testServiceAvailability() throws Exception {
        Assert.assertNotNull(placeholderResolver);
        Assert.assertTrue(placeholderResolver instanceof PlaceholderResolverImpl);

        Assert.assertNotNull(propertyEvaluator);
        Assert.assertTrue(propertyEvaluator instanceof Fabric8PropertyEvaluator);

        Assert.assertNotNull(configurationPlugin);
        Assert.assertTrue(configurationPlugin instanceof PlaceholderResolverConfigurationPlugin);
    }

    @ProbeBuilder
    public TestProbeBuilder probeConfiguration(TestProbeBuilder probe) {
        probe.setHeader(Constants.DYNAMICIMPORT_PACKAGE, "*,org.apache.felix.service.*;status=provisional");
        return probe;
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
            features(
                getFeaturesUrl().toString(),
                "fabric8-karaf-blueprint",
                "fabric8-karaf-cm"),
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
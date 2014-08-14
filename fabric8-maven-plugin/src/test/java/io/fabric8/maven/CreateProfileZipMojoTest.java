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
package io.fabric8.maven;


import io.fabric8.maven.stubs.CreateProfileZipProjectStub;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.junit.Assert;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * This test uses JUnit3 API because the Junit4 API for
 * maven plugin test framework depends on eclipse aether, but we use
 * sonatype aether.
 */
public class CreateProfileZipMojoTest extends AbstractMojoTestCase {

    private CreateProfileZipProjectStub projectStub;

    protected void setUp() throws Exception {
        super.setUp();
        projectStub = new CreateProfileZipProjectStub();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testOverrideClassifier() throws Exception {

        // GIVEN

        pomWithJarPackaging();

        CreateProfileZipMojo mojo = createProfileZipMojoWithBasicConfig();

        // WHEN

        artifactBundleClassifierIsOverridden(mojo);

        mojo.execute();

        // THEN

        bundleReferencesHaveJarWithFooClassifier();
    }

    public void testOverrideType() throws Exception {

        // GIVEN

        pomWithJarPackaging();

        CreateProfileZipMojo mojo = createProfileZipMojoWithBasicConfig();

        // WHEN

        artifactBundleTypeIsOverridden(mojo);

        mojo.execute();

        // THEN

        bundleReferencesHaveZipExtension();

    }

    public void testDefaultType() throws Exception {

        // GIVEN

        pomWithJarPackaging();

        // WHEN

        createProfileZipMojoWithBasicConfig().execute();

        // THEN

        bundleReferencesHaveJarExtension();

    }

    private void bundleReferencesHaveJarExtension() throws IOException {
        Properties props = loadProperties(getFabricAgentPropertiesFile(getGeneratedProfilesDir()));

        assertPropertiesKeyExists(props, getArtifactBundleKey("jar"));
        assertPropertyValue(props, getArtifactBundleKey("jar"),
                getExpectedArtifactBundleValue("jar"));
    }

    private void bundleReferencesHaveJarWithFooClassifier() throws IOException {
        Properties props = loadProperties(getFabricAgentPropertiesFile(getGeneratedProfilesDir()));

        assertPropertiesKeyExists(props, getArtifactBundleKey("jar/foo"));
        assertPropertyValue(props, getArtifactBundleKey("jar/foo"),
                getExpectedArtifactBundleValue("jar/foo"));
    }

    private void bundleReferencesHaveZipExtension() throws IOException {
        Properties props = loadProperties(getFabricAgentPropertiesFile(getGeneratedProfilesDir()));

        assertPropertiesKeyExists(props, getArtifactBundleKey("zip"));
        assertPropertyValue(props, getArtifactBundleKey("zip"),
                getExpectedArtifactBundleValue("zip"));
    }

    private CreateProfileZipMojo createProfileZipMojoWithBasicConfig() throws Exception {
        CreateProfileZipMojo profileZipMojo = (CreateProfileZipMojo) lookupMojo( "zip", getPom());

        assertNotNull(profileZipMojo);

        setVariableValueToObject(profileZipMojo,"buildDir", getGeneratedProfilesDir());

        setVariableValueToObject(profileZipMojo,"outputFile", getProfileZip());

        return profileZipMojo;
    }

    private void artifactBundleTypeIsOverridden(CreateProfileZipMojo mojo) throws IllegalAccessException {
        setVariableValueToObject(mojo, "artifactBundleType", "zip");
    }

    private void artifactBundleClassifierIsOverridden(CreateProfileZipMojo mojo) throws IllegalAccessException {
        setVariableValueToObject(mojo, "artifactBundleClassifier", "foo");
    }

    private void pomWithJarPackaging() {
        Assert.assertEquals("jar", getPackaging());

        Assert.assertEquals("jar", getArtifactType());
    }

    // helpers...

    private void assertPropertyValue(Properties props, String key, String expectedArtifactBundleValue) {
        Assert.assertEquals(expectedArtifactBundleValue, props.getProperty(key));
    }

    private void assertPropertiesKeyExists(Properties props, String key) {
        Assert.assertNotNull("No value for key " + key + ". Available keys: " + props.keySet(), props.getProperty(key));
    }

    private File getProfileZip() {
        return new File(getBasedir() + "/target/profile.zip");
    }

    private File getGeneratedProfilesDir() {
        return new File(getBasedir() + "/target/generated-profiles");
    }

    private String getExpectedArtifactBundleValue(String type) {
        return "fab:mvn:" + getBundleSpec(type);
    }

    private String getArtifactBundleKey(String type) {
        return "bundle.fab:mvn:" + getBundleSpec(type);
    }

    private String getBundleSpec(String type) {
        return getGroupId() + "/" + getArtifactId() + "/" + getVersion() + "/" +type;
    }

    private String getProfilePathComponent() {
        // profilePathComponent: looks like: io.fabric8.maven.test/zip/test.profile
        return getGroupId() + "/" +
                getArtifactId().replace('-', '/') + ".profile";
    }

    private String getVersion() {
        return projectStub.getVersion();
    }

    private String getGroupId() {
        return projectStub.getGroupId();
    }

    private String getArtifactType() {
        return projectStub.getArtifact().getType();
    }

    private String getPackaging() {
        return projectStub.getPackaging();
    }

    private File getFabricAgentPropertiesFile(File generatedProfiles) {
        return new File(generatedProfiles, getProfilePathComponent() +
                "/io.fabric8.agent.properties");
    }

    private String getPom() {
        return projectStub.getFile().toString();
    }

    private String getArtifactId() {
        return projectStub.getArtifactId();
    }

    private Properties loadProperties(File fabricAgentPropertiesFile) throws IOException {
        Properties props = new Properties();
        props.load(new FileInputStream(fabricAgentPropertiesFile));
        return props;
    }

}
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


import io.fabric8.deployer.dto.DependencyDTO;
import io.fabric8.deployer.dto.DtoHelper;
import io.fabric8.deployer.dto.ProjectRequirements;
import io.fabric8.maven.stubs.*;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.junit.Assert;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

/**
 * This test uses JUnit3 API because the Junit4 API for
 * maven plugin test framework depends on eclipse aether, but we use
 * sonatype aether.
 */
public class CreateProfileZipMojoTest extends AbstractMojoTestCase {

    private AbstractProjectStub projectStub;

    public static final String EXPECTED_EXCEPTION_MESSAGE =
            "The property artifactBundleClassifier " +
                    "was specified as '%s' without also specifying artifactBundleType";

    public static final String TEST_CLASSIFIER = "foo";

    /**
     * Special fabric8 deployment agent key prefix associated with various
     * packaging types.
     *
     * E.g. jar is prefixed by 'fab' for historical reasons
     */
    public static final Map<String, String> SPECIAL_KEY_PREFIX_TYPES =
            toMap(entry("jar", "fab:"),entry("war","war:"));

    /**
     * Special pax url prefixes used by the deployer for given packaging types
     *
     * E.g. 'fab' is used for 'jar' for historical reasons
     */
    public static final Map<String,String> SPECIAL_VALUE_PREFIX_TYPES =
            toMap(entry("jar", "fab:"),entry("war","war:"));

    private static Map.Entry<String,String> entry(String key, String value) {
        return new AbstractMap.SimpleImmutableEntry(key,value);
    }

    private static Map<String,String> toMap(Map.Entry<String,String>... entries) {
        HashMap<String,String> newMap = new HashMap<>();
        for (Map.Entry<String,String> e : entries) {
            newMap.put(e.getKey(),e.getValue());
        }
        return Collections.unmodifiableMap(newMap);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testDefaultWarType() throws Exception {

        // GIVEN

        pomWithWarPackaging();

        // WHEN

        createProfileZipMojoWithBasicConfig().execute();

        // THEN

        bundleReferencesHaveWarExtension();

        requirementsRootDependencyPresentWithGAV();
        requirementsRootDependencyHasNoClassifierAndHasType("war");


    }

    public void testAttemptOverrideClassifier() throws Exception {

        // GIVEN

        pomWithJarPackaging();

        CreateProfileZipMojo mojo = createProfileZipMojoWithBasicConfig();

        // WHEN

        artifactBundleClassifierIsOverridden(mojo, TEST_CLASSIFIER);

        // THEN

        expectExceptionWhenExecuting(mojo,
                String.format(EXPECTED_EXCEPTION_MESSAGE, TEST_CLASSIFIER));

    }


    public void testOverrideWithZipType() throws Exception {

        // GIVEN

        pomWithJarPackaging();

        CreateProfileZipMojo mojo = createProfileZipMojoWithBasicConfig();

        // WHEN

        artifactBundleTypeIsOverridden(mojo, "zip");

        mojo.execute();

        // THEN

        bundleReferencesHaveZipExtension();

        requirementsRootDependencyPresentWithGAV();
        requirementsRootDependencyHasNoClassifierAndHasType("zip");

    }

    public void testDefaultJarType() throws Exception {

        // GIVEN

        pomWithJarPackaging();

        // WHEN

        createProfileZipMojoWithBasicConfig().execute();

        // THEN

        bundleReferencesHaveNoExtension();

        requirementsRootDependencyPresentWithGAV();
        requirementsRootDependencyHasNoClassifierAndHasType("jar");

    }

    private void requirementsRootDependencyHasNoClassifierAndHasType(String type) throws IOException {
        Assert.assertEquals(type, getRequirementsRootDependency().getType());
        Assert.assertNull(getRequirementsRootDependency().getClassifier());
    }

    private void requirementsRootDependencyHasTypeAndClassifier(String type, String classifier) throws IOException {
        Assert.assertEquals(type, getRequirementsRootDependency().getType());
        Assert.assertNull(getRequirementsRootDependency().getClassifier());
    }

    private void requirementsRootDependencyPresentWithGAV() throws IOException {
        Assert.assertNotNull(getRequirementsRootDependency());
        Assert.assertEquals(getGroupId(), getRequirementsRootDependency().getGroupId());
        Assert.assertEquals(getArtifactId(), getRequirementsRootDependency().getArtifactId());
        Assert.assertEquals(getVersion(), getRequirementsRootDependency().getVersion());
    }

    private DependencyDTO getRequirementsRootDependency() throws IOException {
        ProjectRequirements requirements = readProjectRequirements();
        return requirements.getRootDependency();
    }


    public void testDefaultBundleType() throws Exception {

        // GIVEN

        pomWithBundlePackaging();

        // WHEN

        createProfileZipMojoWithBasicConfig().execute();

        // THEN

        bundleReferencesHaveNoExtension();

        requirementsRootDependencyPresentWithGAV();
        requirementsRootDependencyHasNoClassifierAndHasType("bundle");

    }

    public void testMuleOverrideType() throws Exception {

        // GIVEN

        pomWithMulePackaging();

        // WHEN

        CreateProfileZipMojo mojo = createProfileZipMojoWithBasicConfig();

        artifactBundleTypeIsOverridden(mojo, "zip");

        mojo.execute();

        // THEN

        bundleReferencesHaveZipExtension();

        requirementsRootDependencyPresentWithGAV();
        requirementsRootDependencyHasNoClassifierAndHasType("zip");

    }

    public void testExplicitJarType() throws Exception {

        // GIVEN

        pomWithJarPackaging();

        CreateProfileZipMojo mojo = createProfileZipMojoWithBasicConfig();

        // WHEN

        artifactBundleTypeIsOverridden(mojo, "jar");

        mojo.execute();

        // THEN

        bundleReferencesHaveJarExtension();

        requirementsRootDependencyPresentWithGAV();
        requirementsRootDependencyHasNoClassifierAndHasType("jar");

    }

    public void testExplicitJarTypeAndOverrideClassifier() throws Exception {

        // GIVEN

        pomWithJarPackaging();

        CreateProfileZipMojo mojo = createProfileZipMojoWithBasicConfig();

        // WHEN

        artifactBundleTypeIsOverridden(mojo, "jar");

        artifactBundleClassifierIsOverridden(mojo, "foo");

        mojo.execute();

        // THEN

        bundleReferencesHaveJarWithFooClassifier();

        requirementsRootDependencyPresentWithGAV();
        requirementsRootDependencyHasTypeAndClassifier("jar","foo");

    }

    private void expectExceptionWhenExecuting(CreateProfileZipMojo mojo, String message) throws MojoExecutionException {
        try {
            mojo.execute();
            Assert.fail("Expected MojoFailureException not thrown");
        } catch (MojoFailureException e) {
            Assert.assertEquals(
                    message,
                    e.getMessage());
        }
    }

    private void bundleReferencesHaveJarExtension() throws IOException {
        Properties props = loadProperties(getFabricAgentPropertiesFile(getGeneratedProfilesDir()));

        assertPropertiesKeyExists(props, getArtifactBundleKey("jar"));
        assertPropertyValue(props, getArtifactBundleKey("jar"),
                getExpectedArtifactBundleValue("jar"));
    }

    private void bundleReferencesHaveWarExtension() throws IOException {
        Properties props = loadProperties(getFabricAgentPropertiesFile(getGeneratedProfilesDir()));

        assertPropertiesKeyExists(props, getArtifactBundleKey("war"));
        assertPropertyValue(props, getArtifactBundleKey("war"),
                getExpectedArtifactBundleValue("war"));
    }

    private void bundleReferencesHaveNoExtension() throws IOException {
        Properties props = loadProperties(getFabricAgentPropertiesFile(getGeneratedProfilesDir()));

        assertPropertiesKeyExists(props, getArtifactBundleKeyNoType());
        assertPropertyValue(props, getArtifactBundleKeyNoType(),
                getExpectedArtifactBundleValueNoType());
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

    private void artifactBundleTypeIsOverridden(CreateProfileZipMojo mojo, String override) throws IllegalAccessException {
        setVariableValueToObject(mojo, "artifactBundleType", override);
    }

    private void artifactBundleClassifierIsOverridden(CreateProfileZipMojo mojo, String classifier) throws IllegalAccessException {
        setVariableValueToObject(mojo, "artifactBundleClassifier", classifier);
    }

    private void pomWithJarPackaging() {
        projectStub = new CreateProfileZipJarProjectStub();

        Assert.assertEquals("jar", getPackaging());

        Assert.assertEquals("jar", getArtifactType());
    }

    private void pomWithWarPackaging() {
        projectStub = new CreateProfileZipWarProjectStub();

        Assert.assertEquals("war", getPackaging());

        Assert.assertEquals("war", getArtifactType());
    }

    private void pomWithBundlePackaging() {
        projectStub = new CreateProfileZipBundleProjectStub();

        Assert.assertEquals("bundle", getPackaging());

        Assert.assertEquals("bundle", getArtifactType());
    }

    private void pomWithMulePackaging() {
        projectStub = new CreateProfileZipMuleProjectStub();

        Assert.assertEquals("mule", getPackaging());

        Assert.assertEquals("mule", getArtifactType());
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

    public String getExpectedAritfactBundleValuePrefix() {
        String packagingType = projectStub.getArtifact().getType();
        return (SPECIAL_VALUE_PREFIX_TYPES.containsKey(packagingType) ? SPECIAL_VALUE_PREFIX_TYPES.get(packagingType) : "") + "mvn:";
    }

    private String getExpectedArtifactBundleValueNoType() {
        return getExpectedAritfactBundleValuePrefix() + getBundleGavSpec();
    }

    private String getExpectedArtifactBundleValue(String type) {
        return getExpectedArtifactBundleValueNoType() + getTypeSpec(type);
    }

    public String getArtfactBundleKeyPrefix() {
        String packagingType = projectStub.getArtifact().getType();
        return ( SPECIAL_KEY_PREFIX_TYPES.containsKey(packagingType) ? SPECIAL_KEY_PREFIX_TYPES.get(packagingType) : "") + "mvn:";
    }

    private String getArtifactBundleKeyNoType() {
        return "bundle."+getArtfactBundleKeyPrefix() + getBundleGavSpec();
    }

    private String getArtifactBundleKey(String type) {
        return getArtifactBundleKeyNoType() + getTypeSpec(type);
    }

    private String getTypeSpec(String type) {
        return "/" +type;
    }

    private String getBundleGavSpec() {
        return getGroupId() + "/" + getArtifactId() + "/" + getVersion();
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

    private File getProjectRequirementsFile(File generatedProfiles) {
        return new File(generatedProfiles, getProfilePathComponent() +
                "/dependencies/" + getGroupId() +
                "/" + getArtifactId() + "-requirements.json");
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

    private ProjectRequirements readProjectRequirements() throws IOException {
        byte[] data = Files.readAllBytes(getProjectRequirementsFile(getGeneratedProfilesDir()).toPath());
        return DtoHelper.getMapper().readValue(data, ProjectRequirements.class);
    }

    private Properties loadProperties(File fabricAgentPropertiesFile) throws IOException {
        Properties props = new Properties();
        props.load(new FileInputStream(fabricAgentPropertiesFile));
        return props;
    }

}
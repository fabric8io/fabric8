package io.fabric8.maven;

import io.fabric8.deployer.dto.DependencyDTO;
import io.fabric8.deployer.dto.ProjectRequirements;
import io.fabric8.utils.Files;
import io.fabric8.utils.Strings;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;

/**
 * Generates a ZIP file of the profile configuration
 */
@Mojo(name = "zip", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
@Execute(phase = LifecyclePhase.PACKAGE)
public class CreateProfileZipMojo extends AbstractProfileMojo {

    /**
     * Name of the directory used to create the profile configuration zip
     */
    @Parameter(property = "fabric8.zip.buildDir", defaultValue = "${project.build.directory}/generated-profiles")
    private File buildDir;

    /**
     * Name of the created profile zip file
     */
    @Parameter(property = "fabric8.zip.outFile", defaultValue = "${project.build.directory}/profile.zip")
    private File outputFile;

    @Component
    private MavenProjectHelper projectHelper;

    /**
     * The artifact type for attaching the generated profile zip file to the project
     */
    @Parameter(property = "fabric8.zip.artifactType", defaultValue = "zip")
    private String artifactType = "karaf";

    /**
     * The artifact classifier for attaching the generated profile zip file to the project
     */
    @Parameter(property = "fabric8.zip.artifactClassifier", defaultValue = "profile")
    private String artifactClassifier = "profile";

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            ProjectRequirements requirements = new ProjectRequirements();

            DependencyDTO rootDependency = null;
            if (!"pom".equals(project.getPackaging())) {
                rootDependency = loadRootDependency();
                requirements.setRootDependency(rootDependency);
            }
            configureRequirements(requirements);

            File profileBuildDir = createProfileBuildDir(requirements.getProfileId());

            boolean hasConfigDir = profileConfigDir.isDirectory();
            if (hasConfigDir) {
                copyProfileConfigFiles(profileBuildDir, profileConfigDir);
            } else {
                getLog().info("The profile configuration files directory " + profileConfigDir + " doesn't exist, so not copying any additional project documentation or configuration files");
            }

            // lets only generate a profile zip if  we have a requirement (e.g. we're not a parent pom packaging project) and
            // we have defined some configuration files or dependencies
            // to avoid generating dummy profiles for parent poms
            if (hasConfigDir || rootDependency != null ||
                    notEmpty(requirements.getBundles()) || notEmpty(requirements.getFeatures()) || notEmpty(requirements.getFeatureRepositories())) {
                generateFabricAgentProperties(requirements, new File(profileBuildDir, "io.fabric8.agent.properties"));

                Zips.createZipFile(getLog(), buildDir, outputFile);

                projectHelper.attachArtifact(project, artifactType, artifactClassifier, outputFile);

                String relativePath = Files.getRelativePath(project.getBasedir(), outputFile);
                while (relativePath.startsWith("/")) {
                    relativePath = relativePath.substring(1);
                }
                getLog().info("Created profile zip file: " + relativePath);
            }
        } catch (MojoExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("Error executing", e);
        }
    }

    public static boolean notEmpty(List<?> list) {
        return list != null && !list.isEmpty();
    }

    /**
     * Copies any local configuration files into the profile directory
     */
    protected void copyProfileConfigFiles(File profileBuildDir, File profileConfigDir) throws IOException {
        File[] files = profileBuildDir.listFiles();
        if (files != null) {
            profileConfigDir.mkdirs();
            for (File file : files) {
                File outFile = new File(profileConfigDir, file.getName());
                if (file.isDirectory()) {
                    copyProfileConfigFiles(file, outFile);
                } else {
                    Files.copy(file, outFile);
                }
            }
        }

    }

    /**
     * Returns the directory within the {@link #buildDir} to generate data for the profile
     */
    protected File createProfileBuildDir(String profileId) {
        String profilePath = profileId.replace('-', '/') + ".profile";
        return new File(buildDir, profilePath);
    }

    protected void generateFabricAgentProperties(ProjectRequirements requirements, File file) throws MojoExecutionException, IOException {
        file.getParentFile().mkdirs();

        PrintWriter writer = new PrintWriter(new FileWriter(file));
        try {
            String profileId = requirements.getProfileId();
            writer.println("# Profile: " +  profileId);
            writer.println("# generated by the fabric8 maven plugin at " + new Date());
            writer.println("# see: http://fabric8.io/#/site/book/doc/index.md?chapter=mavenPlugin_md");
            writer.println();
            List<String> parentProfiles = Zips.notNullList(requirements.getParentProfiles());
            if (!parentProfiles.isEmpty()) {
                writer.write("attribute.parents =");
                for (String parentProfile : parentProfiles) {
                    writer.write(" ");
                    writer.write(parentProfile);
                }
                writer.println();
                writer.println();
            }
            List<String> bundles = Zips.notNullList(requirements.getBundles());
            List<String> features = Zips.notNullList(requirements.getFeatures());
            List<String> repos = Zips.notNullList(requirements.getFeatureRepositories());
            for (String bundle : bundles) {
                if (Strings.isNotBlank(bundle)) {
                    writer.println("bundle." + escapeAgentPropertiesKey(bundle) + " = " + escapeAgentPropertiesValue(bundle));
                }
            }
            if (!bundles.isEmpty()) {
                writer.println();
            }
            for (String feature : features) {
                if (Strings.isNotBlank(feature)) {
                    writer.println("feature." + escapeAgentPropertiesKey(feature) + " = " + escapeAgentPropertiesValue(feature));
                }
            }
            if (!features.isEmpty()) {
                writer.println();
            }
            for (String repo : repos) {
                if (Strings.isNotBlank(repo)) {
                    writer.println("repository." + escapeAgentPropertiesKey(repo) + " = " + escapeAgentPropertiesValue(repo));
                }
            }
        } finally {
            try {
                writer.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    protected String escapeAgentPropertiesKey(String text) {
        return text.replaceAll("\\:", "\\\\:");
    }

    protected String escapeAgentPropertiesValue(String text) {
        return escapeAgentPropertiesKey(text);
    }

}

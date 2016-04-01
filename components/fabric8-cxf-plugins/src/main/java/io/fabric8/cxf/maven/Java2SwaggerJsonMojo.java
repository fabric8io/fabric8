/**
 *  Copyright 2005-2016 Red Hat, Inc.
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

package io.fabric8.cxf.maven;

import io.fabric8.cxf.endpoint.SwaggerFeature;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.helpers.FileUtils;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * @goal java2swagger
 * @description CXF Java To swagger json payload Tool
 * @requiresDependencyResolution test
 * @threadSafe
 */
public class Java2SwaggerJsonMojo extends AbstractMojo {


    /**
     * @parameter
     */
    private String outputFile;


    /**
     * Attach the generated swagger json file to the list of files to be deployed
     * on install. This means the swagger json file will be copied to the repository
     * with groupId, artifactId and version of the project and type "json".
     * <p/>
     * With this option you can use the maven repository as a Service Repository.
     *
     * @parameter default-value="true"
     */
    private Boolean attachSwagger;


    /**
     * @parameter
     */
    private String classifier;

    /**
     * @parameter
     * @required
     */
    private List<String> classResourceNames;


    /**
     * @parameter expression="${project}"
     * @required
     */
    private MavenProject project;


    /**
     * Maven ProjectHelper.
     *
     * @component
     * @readonly
     */
    private MavenProjectHelper projectHelper;


    /**
     * @parameter
     */
    private String outputFileName;

    /**
     * @parameter default-value="json"
     */
    private String outputFileExtension;

    /**
     * @parameter default-value="http://localhost:12333/cxf/swagger"
     */
    private String address;

    private ClassLoader resourceClassLoader;


    public void execute() throws MojoExecutionException {
        List<Class<?>> resourceClasses = loadResourceClasses();
        List<Object> resourceObjects = new ArrayList<Object>();
        for (Class<?> resourceClass : resourceClasses) {
            try {
                resourceObjects.add(resourceClass.newInstance());
            } catch (InstantiationException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            } catch (IllegalAccessException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }

        Thread.currentThread().setContextClassLoader(getClassLoader());
        List<Feature> features = new ArrayList<Feature>();
        features.add(new SwaggerFeature());
        JAXRSServerFactoryBean serverFacBean = new JAXRSServerFactoryBean();
        serverFacBean.setAddress(address);
        serverFacBean.setServiceBeans(resourceObjects);
        serverFacBean.setFeatures(features);
        Server server = serverFacBean.create();

        InputStream in = null;
        try {
            String serverAddress = server.getEndpoint().getEndpointInfo().getAddress();
            String apiDocs = serverAddress + "/swagger.json";
            URL url = new URL(apiDocs);
            in = url.openStream();
            String res = getStringFromInputStream(in);
            generateJson(resourceClasses, res);
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } finally {
            server.stop();
        }


    }


    private void generateJson(List<Class<?>> resourceClasses, String swagger) throws MojoExecutionException {

        if (outputFile == null && project != null) {
            // Put the json in target/generated/json

            String name = null;
            if (outputFileName != null) {
                name = outputFileName;
            } else if (resourceClasses.size() == 1) {
                name = resourceClasses.get(0).getSimpleName();
            } else {
                name = "application";
            }
            outputFile = (project.getBuild().getDirectory() + "/generated/json/" + name + "."
                    + outputFileExtension).replace("/", File.separator);
        }

        BufferedWriter writer = null;
        try {
            FileUtils.mkDir(new File(outputFile).getParentFile());
            writer = new BufferedWriter(new FileWriter(outputFile));
            writer.write(swagger);

        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }
        // Attach the generated json file to the artifacts that get deployed
        // with the enclosing project
        if (attachSwagger && outputFile != null) {
            File jsonFile = new File(outputFile);
            if (jsonFile.exists()) {
                if (classifier != null) {
                    projectHelper.attachArtifact(project, "json", classifier, jsonFile);
                } else {
                    projectHelper.attachArtifact(project, "json", jsonFile);
                }

            }
        }
    }


    private ClassLoader getClassLoader() throws MojoExecutionException {
        if (resourceClassLoader == null) {
            try {
                List<?> runtimeClasspathElements = project.getRuntimeClasspathElements();
                URL[] runtimeUrls = new URL[runtimeClasspathElements.size()];
                for (int i = 0; i < runtimeClasspathElements.size(); i++) {
                    String element = (String) runtimeClasspathElements.get(i);
                    runtimeUrls[i] = new File(element).toURI().toURL();
                }
                resourceClassLoader = new URLClassLoader(runtimeUrls, Thread.currentThread()
                        .getContextClassLoader());
            } catch (Exception e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }
        return resourceClassLoader;
    }

    private List<Class<?>> loadResourceClasses() throws MojoExecutionException {
        List<Class<?>> resourceClasses = new ArrayList<Class<?>>(classResourceNames.size());
        for (String className : classResourceNames) {
            try {
                resourceClasses.add(getClassLoader().loadClass(className));
            } catch (Exception e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }

        return resourceClasses;
    }


    private static String getStringFromInputStream(InputStream in) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int c = 0;
        while ((c = in.read()) != -1) {
            bos.write(c);
        }
        in.close();
        bos.close();
        return bos.toString();
    }

}

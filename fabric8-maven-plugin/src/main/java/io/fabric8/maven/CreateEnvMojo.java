/*
 * Copyright 2005-2014 Red Hat, Inc.                                    
 *                                                                      
 * Red Hat licenses this file to you under the Apache License, version  
 * 2.0 (the "License"); you may not use this file except in compliance  
 * with the License.  You may obtain a copy of the License at           
 *                                                                      
 *    http://www.apache.org/licenses/LICENSE-2.0                        
 *                                                                      
 * Unless required by applicable law or agreed to in writing, software  
 * distributed under the License is distributed on an "AS IS" BASIS,    
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or      
 * implied.  See the License for the specific language governing        
 * permissions and limitations under the License.
 */
package io.fabric8.maven;

import io.fabric8.kubernetes.api.KubernetesClient;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteList;
import io.fabric8.utils.Files;
import io.fabric8.utils.TablePrinter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Generates a properties file that contains that env variables that are expected to be passed by kubernetes to the container.
 * The env variable configuration is determined by the kubernetes.json file and the services that are currently running in the target namespace.
 */
@Mojo(name = "create-env", defaultPhase = LifecyclePhase.COMPILE)
public class CreateEnvMojo extends AbstractFabric8Mojo {

    private static final String HOST_SUFFIX = "_SERVICE_HOST";
    private static final String PORT_SUFFIX = "_SERVICE_PORT";
    private static final String PROTO_SUFFIX = "_TCP_PROTO";
    private static final String DOCKE_ENV_PREFIX = "docker.env.";
    private static final String DOCKER_NAME = "docker.name";
    private static final String EXEC_ENV_SCRIPT = "environmentScript";

    
    private final KubernetesClient kubernetes = new KubernetesClient();

    @Parameter(property = "fabric8.namespace", defaultValue = "default")
    private String namespace;

    @Parameter(property = "fabric8.envFile", defaultValue = "env.properties")
    private String envPropertiesFileName;

    @Parameter(property = "fabric8.envScript", defaultValue = "env.sh")
    private String envScriptFileName;

    @Parameter(property = "docker.image")
    private String name;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            String basedir = System.getProperty("basedir", ".");
            File propertiesFile = new File(basedir + "/target/" + envPropertiesFileName).getCanonicalFile();
            File scriptFile = new File(basedir + "/target/" + envScriptFileName).getCanonicalFile();

            Object config = loadKubernetesJson();
            List<Object> list = KubernetesHelper.toItemList(config);
            Map<String, String> env = getEnvFromConfig(list);
            env.putAll(getNamespaceServiceEnv(namespace));
            displayEnv(env);
            
            for (Map.Entry<String, String> entry : env.entrySet()) {
                getProject().getProperties().setProperty(DOCKE_ENV_PREFIX + entry.getKey(), entry.getValue());
            }
            getProject().getProperties().setProperty(DOCKER_NAME, name);
            getProject().getProperties().setProperty(EXEC_ENV_SCRIPT, scriptFile.getAbsolutePath());
            Properties envProperties = new Properties();
            envProperties.putAll(env);
            saveScript(env, scriptFile);
            saveProperties(envProperties, propertiesFile);

        } catch (IOException e) {
            throw new MojoExecutionException("Failed to load environment schemas: " + e, e);
        }
    }

    /**
     * Return the Env Variables that correspond to each services that runs under the specified namespace.
     * @param namespace The target namespace.
     * @return
     */
     Map<String, String> getNamespaceServiceEnv(String namespace) {
        Map<String, String> result = new HashMap<>();
        ServiceList serviceList = kubernetes.getServices(namespace);
        RouteList routeList = kubernetes.getRoutes(namespace);
        for (Service service : serviceList.getItems()) {
            String id = service.getId().toUpperCase().replace("-", "_");
            Route route = findRoute(service.getId(), routeList);
            if (route != null) {
                result.put(id + HOST_SUFFIX, route.getHost());
                result.put(id + PORT_SUFFIX, String.valueOf(service.getPort()));
                result.put(id + PROTO_SUFFIX, service.getProtocol());
                result.put(id + PORT_SUFFIX + "_" + service.getPort() + PROTO_SUFFIX, service.getProtocol());
            } else {
                result.put(id + HOST_SUFFIX, service.getPortalIP());
                result.put(id + PORT_SUFFIX, String.valueOf(service.getPort()));
                result.put(id + PROTO_SUFFIX, service.getProtocol());
                result.put(id + PORT_SUFFIX + "_" + service.getPort() + PROTO_SUFFIX, service.getProtocol());
            }
        }
        return result;
    }


    /**
     * Return Env variables found in the kubernetes config.
     *
     * @param config The config instance.
     * @return A map with the env key value pairs.
     * @throws IOException
     */
    private Map<String, String> getEnvFromConfig(List<Object> entities) throws IOException {
        Map<String, String> result = new HashMap<>();

        for (Object entity : entities) {
            if (entity instanceof Pod) {
                Pod pod = (Pod) entity;
                for (Container container : pod.getDesiredState().getManifest().getContainers()) {
                    if (container.getImage().equals(name)) {
                        result.putAll(mapFromEnv(container.getEnv()));
                    }
                }
            } else if (entity instanceof ReplicationController) {
                ReplicationController replicationController = (ReplicationController) entity;
                for (Container container : replicationController.getDesiredState().getPodTemplate().getDesiredState().getManifest().getContainers()) {
                    if (container.getImage().equals(name)) {
                        result.putAll(mapFromEnv(container.getEnv()));
                    }
                }
            }
        }
        return result;
    }

    private void displayEnv(Map<String, String> map) {
        TablePrinter table = new TablePrinter();
        table.columns("Name", "Value");

        for (Map.Entry<String, String> entry : map.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();
            table.row(name, value);
        }

        getLog().info("");
        getLog().info("Generated Environment variables:");
        getLog().info("-------------------------------");

        List<String> lines = table.asTextLines();
        for (String line : lines) {
            getLog().info(line);
        }
        getLog().info("");

    }
    
    /**
     * Load the kubernetes configuration found in the project
     *
     * @return The Kubernetes config.
     * @throws MojoFailureException
     * @throws IOException
     */
    private Object loadKubernetesJson() throws MojoFailureException, IOException {
        File json = getKubernetesJson();
        if (!Files.isFile(json)) {
            if (Files.isFile(kubernetesSourceJson)) {
                json = kubernetesSourceJson;
            } else {
                throw new MojoFailureException("No such generated kubernetes json file: " + json + " or source json file " + kubernetesSourceJson);
            }
        }
        return KubernetesHelper.loadJson(json);
    }

    private static Map<String, String> mapFromEnv(List<EnvVar> envVars) {
        Map<String, String> result = new HashMap<>();
        for (EnvVar envVar : envVars) {
            result.put(envVar.getName(), envVar.getValue());
        }
        return result;
    }

    private static void saveProperties(Properties properties, File propertiesFile) throws IOException {
        try (FileWriter writer = new FileWriter(propertiesFile)) {
            properties.store(writer, "Generated Environment Variables");
        }
    }

    private static void saveScript(Map<String, String> map, File scroptFile) throws IOException {
        try (FileWriter writer = new FileWriter(scroptFile)) {
            writer.append("#!/bin/bash").append("\n");
            for (Map.Entry<String, String> entry: map.entrySet()) {
                writer.append("export ").append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
            }
            writer.flush();
        }
    }

    private static Route findRoute(String serviceId, RouteList routeList) {
        for (Route route : routeList.getItems()) {
            if (route.getServiceName().equals(serviceId)) {
                return route;
            }
        }
        return null;
    }
}

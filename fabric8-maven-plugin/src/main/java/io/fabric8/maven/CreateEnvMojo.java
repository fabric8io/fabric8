/*
 * Copyright 2005-2015 Red Hat, Inc.                                    
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

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectReference;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerSpec;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.maven.support.DefaultExcludedEnvVariablesEnum;
import io.fabric8.maven.support.DockerCommandPlainPrint;
import io.fabric8.maven.support.IDockerCommandPlainPrintCostants;
import io.fabric8.maven.support.OrderedProperties;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteList;
import io.fabric8.openshift.api.model.RouteListBuilder;
import io.fabric8.openshift.api.model.RouteSpec;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.utils.Files;
import io.fabric8.utils.Strings;
import io.fabric8.utils.TablePrinter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

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

    @Parameter(property = "fabric8.envFile", defaultValue = "env.properties")
    private String envPropertiesFileName;

    @Parameter(property = "fabric8.envScript", defaultValue = "env.sh")
    private String envScriptFileName;
    
    @Parameter(property = "fabric8.dockerRunScript", defaultValue = "docker-run.sh")
    private String dockerRunScriptFileName;

    // the docker image name we will use
    private volatile String name;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            String basedir = System.getProperty("basedir", ".");
            File propertiesFile = new File(basedir + "/target/" + envPropertiesFileName).getCanonicalFile();
            File scriptFile = new File(basedir + "/target/" + envScriptFileName).getCanonicalFile();
            File dockerRunFile = new File(basedir + "/target/" + dockerRunScriptFileName).getCanonicalFile();
            
            Object config = loadKubernetesJson();
            List<HasMetadata> list = KubernetesHelper.toItemList(config);
            name = getDockerImage();
            if (name == null) {
                name = findFirstImageName(list);
            }
            Map<String, String> env = getEnvFromConfig(list);
            String namespace = getNamespace();
            env.putAll(getNamespaceServiceEnv(namespace));
            removeDefaultEnv(env);
            expandEnvironmentVariable(env);
            displayEnv(env);

            StringBuilder sb = new StringBuilder();
            List<VolumeMount> volumeMount = getVolumeMountsFromConfig(list);
            List<ContainerPort> containerPort = getContainerPortsFromConfig(list);
            DockerCommandPlainPrint dockerCommandPlainPrint = new DockerCommandPlainPrint(sb);
            dockerCommandPlainPrint.appendParameters(env, IDockerCommandPlainPrintCostants.EXPRESSION_FLAG);
            dockerCommandPlainPrint.appendContainerPorts(containerPort, IDockerCommandPlainPrintCostants.PORT_FLAG);
            dockerCommandPlainPrint.appendVolumeMounts(volumeMount, IDockerCommandPlainPrintCostants.VOLUME_FLAG);
            dockerCommandPlainPrint.appendImageName(name);
            
            displayVolumes(volumeMount);
            displayContainerPorts(containerPort);
            displayDockerRunCommand(dockerCommandPlainPrint);

            Properties properties = getProjectAndFabric8Properties(getProject());
            for (Map.Entry<String, String> entry : env.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (key == null) {
                    getLog().warn("Ignoring null key!");
                } else if (value == null) {
                    getLog().warn("Ignoring null value for key: " + key);
                } else {
                    properties.setProperty(DOCKE_ENV_PREFIX + key, value);
                }
            }
            if (name != null) {
                properties.setProperty(DOCKER_NAME, name);
            }
            String scriptFileAbsolutePath = scriptFile.getAbsolutePath();
            if (scriptFileAbsolutePath != null) {
                properties.setProperty(EXEC_ENV_SCRIPT, scriptFileAbsolutePath);
            }
            Properties envProperties = new OrderedProperties();
            Set<Map.Entry<String, String>> entries = env.entrySet();
            for (Map.Entry<String, String> entry : entries) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (key == null) {
                    getLog().warn("Ignoring null key!");
                } else if (value == null) {
                    getLog().warn("Ignoring null value for key: " + key);
                } else {
                    envProperties.put(key ,value);
                }
            }
            saveEnvScript(env, scriptFile);
            saveDockerRunScript(dockerCommandPlainPrint, dockerRunFile);
            saveProperties(envProperties, propertiesFile);

        } catch (IOException e) {
            throw new MojoExecutionException("Failed to load environment schemas: " + e, e);
        }
    }

    /**
     * Returns the first docker image name found in a ReplicationController
     */
    protected String findFirstImageName(List<HasMetadata> list) {
        for (HasMetadata hasMetadata : list) {
            if (hasMetadata instanceof ReplicationController) {
                ReplicationController rc = (ReplicationController) hasMetadata;
                ReplicationControllerSpec spec = rc.getSpec();
                if (spec != null) {
                    PodTemplateSpec podTemplateSpec = spec.getTemplate();
                    if (podTemplateSpec != null) {
                        PodSpec podSpec = podTemplateSpec.getSpec();
                        if (podSpec != null) {
                            List<Container> containers = podSpec.getContainers();
                            if (containers != null) {
                                for (Container container : containers) {
                                    String image = container.getImage();
                                    if (Strings.isNotBlank(image)) {
                                        return image;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Return the Env Variables that correspond to each services that runs under the specified namespace.
     *
     * @param namespace The target namespace.
     * @return the env variables
     */
    Map<String, String> getNamespaceServiceEnv(String namespace) {
        Map<String, String> result = new HashMap<>();
        KubernetesClient kubernetes = getKubernetes();
        ServiceList serviceList = kubernetes.services().inNamespace(namespace).list();
        RouteList routeList = listRoutes(kubernetes, namespace);
        for (Service service : serviceList.getItems()) {
            String serviceName = KubernetesHelper.getName(service);
            String id = serviceName.toUpperCase().replace("-", "_");
            Route route = findRoute(serviceName, routeList);
            RouteSpec spec = null;
            if (route != null) {
                spec = route.getSpec();
            }
            ServiceSpec serviceSpec = service.getSpec();
            if (spec != null) {
                result.put(id + HOST_SUFFIX, spec.getHost());
            } else if (serviceSpec != null) {
                result.put(id + HOST_SUFFIX, serviceSpec.getClusterIP());
            }
            if (serviceSpec != null) {
                List<ServicePort> ports = serviceSpec.getPorts();
                for (ServicePort port : ports) {
                    result.put(id + PORT_SUFFIX, String.valueOf(port.getPort()));
                    result.put(id + PROTO_SUFFIX, port.getProtocol());
                    result.put(id + PORT_SUFFIX + "_" + port.getPort() + PROTO_SUFFIX, port.getProtocol());
                }
            }
        }
        return result;
    }




    /**
     * Return Env variables found in the kubernetes config.
     *
     * @param entities The config instance.
     * @return A map with the env key value pairs.
     * @throws IOException
     */
    private Map<String, String> getEnvFromConfig(List<HasMetadata> entities) throws IOException {
        Map<String, String> result = new TreeMap<>();

        for (HasMetadata entity : entities) {
            if (entity instanceof Pod) {
                Pod pod = (Pod) entity;
                for (Container container : pod.getSpec().getContainers()) {
                    if (container.getImage().equals(name)) {
                        result.putAll(mapFromEnv(container.getEnv()));
                    }
                }
            } else if (entity instanceof ReplicationController) {
                ReplicationController replicationController = (ReplicationController) entity;
                for (Container container : replicationController.getSpec().getTemplate().getSpec().getContainers()) {
                    if (container.getImage().equals(name)) {
                        result.putAll(mapFromEnv(container.getEnv()));
                    }
                }
            }
        }
        return result;
    }
    
	/**
	 * Lets expand environment variables by overriding it via via the command
	 * line.
	 */
	protected void expandEnvironmentVariable(Map<String, String> env) {
		String regex = "\\$\\{(.*?)\\}";
		MavenProject project = getProject();
		if (project != null) {
			Properties properties = getProjectAndFabric8Properties(project);
			for (Map.Entry<String, String> entry : env.entrySet()) {
				String envValue = entry.getValue();
				if (envValue != null && !envValue.isEmpty() && envValue.contains("${") && envValue.contains("}")) {
					Pattern p = Pattern.compile(regex);
					Matcher m = p.matcher(entry.getValue());
					while (m.find()) {
						String parameterName = m.group(1);
						String name = "fabric8.create-env." + parameterName;
						String propertyValue = properties.getProperty(name);
						if (Strings.isNotBlank(propertyValue)) {
							getLog().info("Overriding environment variable " + parameterName + " with value: " + propertyValue);
							String replaced = checkAndReplaceValue(entry.getValue(), propertyValue, parameterName);
							entry.setValue(replaced);
						} else {
							getLog().info("No property defined for environment variable: " + parameterName);
						}
					}
				}
			}
		}
	}
    
	/**
	 * Return VolumeMounts found in the kubernetes config.
	 *
	 * @param entities The config instance.
	 * @return A list of VolumeMount objects.
	 * @throws IOException
	 */
	private List<VolumeMount> getVolumeMountsFromConfig(List<HasMetadata> entities) throws IOException {
		List<VolumeMount> volumeList = new ArrayList<VolumeMount>();

		for (HasMetadata entity : entities) {
			if (entity instanceof ReplicationController) {
				ReplicationController replicationController = (ReplicationController) entity;
				for (Container container : replicationController.getSpec().getTemplate().getSpec().getContainers()) {
					if (container.getImage().equals(name)) {
						if (!container.getVolumeMounts().isEmpty()) {
							return container.getVolumeMounts();
						}
					}
				}
			}
		}
		return volumeList;
	}
		
	/**
	 * Return container Ports found in the kubernetes config.
	 *
	 * @param entities The config instance.
	 * @return A list of ContainerPort objects.
	 * @throws IOException
	 */
	private List<ContainerPort> getContainerPortsFromConfig(List<HasMetadata> entities) throws IOException {
		List<ContainerPort> containerPortList = new ArrayList<ContainerPort>();

		for (HasMetadata entity : entities) {
            if (entity instanceof Pod) {
                Pod pod = (Pod) entity;
                for (Container container : pod.getSpec().getContainers()) {
                    if (container.getImage().equals(name)) {
                    	if (!container.getPorts().isEmpty()) {
                    	    containerPortList.addAll(container.getPorts());
                    	}
                    }
                }
            }
            else if (entity instanceof ReplicationController) {
					ReplicationController replicationController = (ReplicationController) entity;
					for (Container container : replicationController.getSpec().getTemplate().getSpec().getContainers()) {
						if (container.getImage().equals(name)) {
							if (!container.getPorts().isEmpty()) {
								containerPortList.addAll(container.getPorts());
							}
						}
					}
			    }
		    } 
		    return containerPortList;
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
    
    private void displayVolumes(List<VolumeMount> volumeMount) {
        TablePrinter table = new TablePrinter();
        table.columns("Name", "Mount Path", "Read Only");

		Iterator<VolumeMount> it = volumeMount.iterator();
		while (it.hasNext()){
			VolumeMount vol = it.next();
            String name = vol.getName();
            String mounthPath = vol.getMountPath();
            String ro = String.valueOf(Boolean.FALSE);
            if (vol.getReadOnly() == true) {
            	ro = String.valueOf(Boolean.TRUE);
            }
			table.row(name, mounthPath, ro);
		}

        getLog().info("");
        getLog().info("Volumes Summary:");
        getLog().info("-------------------------------");

        List<String> lines = table.asTextLines();
        for (String line : lines) {
            getLog().info(line);
        }
        getLog().info("");

    }
    
    private void displayContainerPorts(List<ContainerPort> containerPort) {
        TablePrinter table = new TablePrinter();
        table.columns("Host IP", "Host Port", "Container Port");

		Iterator<ContainerPort> it = containerPort.iterator();
		while (it.hasNext()){
			ContainerPort port = it.next();
            String hostIp = port.getHostIP();
            String hostPort = "";
            String contPort = "";
            if (port.getHostPort() != null) {
            	hostPort = String.valueOf(port.getHostPort());
            }
            if (port.getContainerPort() != null) {
            	contPort = String.valueOf(port.getContainerPort());
            }
			table.row(hostIp, hostPort, contPort);
		}

        getLog().info("");
        getLog().info("Container Ports Summary:");
        getLog().info("-------------------------------");

        List<String> lines = table.asTextLines();
        for (String line : lines) {
            getLog().info(line);
        }
        getLog().info("");

    }
    
    private void removeDefaultEnv(Map<String, String> map) {
        for(Iterator<Map.Entry<String, String>> it = map.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, String> entry = it.next();
            if(DefaultExcludedEnvVariablesEnum.contains(entry.getKey())) {
                it.remove();
            }
        }
    }
    
    private void displayDockerRunCommand(DockerCommandPlainPrint dockerCommandPlainPrint) {
        getLog().info("Docker Run Command:");
        getLog().info("-------------------------------");
        getLog().info(dockerCommandPlainPrint.getDockerPlainTextCommand().toString());
    }
    
    /**
     * Load the kubernetes configuration found in the project
     *
     * @return The Kubernetes config.
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

    private static void saveEnvScript(Map<String, String> map, File scroptFile) throws IOException {
        try (FileWriter writer = new FileWriter(scroptFile)) {
            writer.append("#!/bin/bash").append("\n");
            for (Map.Entry<String, String> entry: map.entrySet()) {
            	if (entry.getValue() != null && !entry.getValue().contains(" ")) {
                    writer.append("export ").append(entry.getKey()).append("=").append(entry.getValue()).append("\n");	
            	} else {
                    writer.append("export ").append(entry.getKey()).append("=").append("\"").append(entry.getValue()).append("\"").append("\n");
            	}
            }
            writer.flush();
        }
    }
    
    private static void saveDockerRunScript(DockerCommandPlainPrint printer, File scriptFile) throws IOException {
        try (FileWriter writer = new FileWriter(scriptFile)) {
            writer.append("#!/bin/bash").append("\n");
            writer.append(printer.getDockerPlainTextCommand().toString()).append("\n");
            writer.flush();
        }
    }

    private static RouteList listRoutes(KubernetesClient client, String namespace) {
        try {
            return client.adapt(OpenShiftClient.class).routes().inNamespace(namespace).list();
        } catch (Throwable t) {
            return new RouteListBuilder().build();
        }
    }

    private static Route findRoute(String serviceId, RouteList routeList) {
        for (Route route : routeList.getItems()) {
            RouteSpec spec = route.getSpec();
            if (spec != null) {
                ObjectReference to = spec.getTo();
                if (to != null) {
                    String name = to.getName();
                    if (serviceId.equals(name)) {
                        return route;
                    }
                }
            }
        }
        return null;
    }
    
    private static String checkAndReplaceValue(String envValue, String value, String parameterName) {
    	String escapedParameterName = "\\$\\{" + parameterName + "\\}";
    	String stringReplaced = envValue.replaceAll(escapedParameterName, value);
    	return stringReplaced;
    }
}
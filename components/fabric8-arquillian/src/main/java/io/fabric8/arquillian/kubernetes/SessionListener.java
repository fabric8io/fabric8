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
package io.fabric8.arquillian.kubernetes;

import io.fabric8.arquillian.kubernetes.await.CompositeCondition;
import io.fabric8.arquillian.kubernetes.await.SessionPodsAreReady;
import io.fabric8.arquillian.kubernetes.await.SessionServicesAreReady;
import io.fabric8.arquillian.kubernetes.await.WaitStrategy;
import io.fabric8.arquillian.kubernetes.event.Start;
import io.fabric8.arquillian.kubernetes.event.Stop;
import io.fabric8.arquillian.kubernetes.log.Logger;
import io.fabric8.arquillian.utils.Commands;
import io.fabric8.arquillian.utils.Routes;
import io.fabric8.arquillian.utils.SecretKeys;
import io.fabric8.arquillian.utils.Secrets;
import io.fabric8.arquillian.utils.URLs;
import io.fabric8.arquillian.utils.Util;
import io.fabric8.kubernetes.api.Controller;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.builder.Visitable;
import io.fabric8.kubernetes.api.builder.Visitor;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.ReplicaSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.internal.HasMetadataComparator;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.ImageStream;
import io.fabric8.openshift.api.model.OAuthClient;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.Template;
import io.fabric8.openshift.client.OpenShiftAPIGroups;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.utils.Files;
import io.fabric8.utils.MultiException;
import io.fabric8.utils.Strings;
import org.jboss.arquillian.core.api.annotation.Observes;

import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.fabric8.arquillian.kubernetes.Configuration.findConfigResource;
import static io.fabric8.arquillian.utils.Namespaces.checkNamespace;
import static io.fabric8.arquillian.utils.Namespaces.createNamespace;
import static io.fabric8.arquillian.utils.ConfigMaps.updateConfigMapStatus;
import static io.fabric8.arquillian.utils.Util.cleanupSession;
import static io.fabric8.arquillian.utils.Util.displaySessionStatus;
import static io.fabric8.arquillian.utils.Util.readAsString;
import static io.fabric8.kubernetes.api.KubernetesHelper.getName;
import static io.fabric8.kubernetes.api.KubernetesHelper.loadJson;
import static io.fabric8.kubernetes.api.KubernetesHelper.loadYaml;
import static io.fabric8.kubernetes.api.extensions.Templates.overrideTemplateParameters;

public class SessionListener {
    private ShutdownHook shutdownHook;
    private DependencyResolver resolver = new DependencyResolver();

    public void start(final @Observes Start event, KubernetesClient client, Controller controller, Configuration configuration) throws Exception {
        Objects.requireNonNull(client, "KubernetesClient most not be null!");
        Session session = event.getSession();
        final Logger log = session.getLogger();
        String namespace = session.getNamespace();
        System.setProperty(Constants.KUBERNETES_NAMESPACE, namespace);

        log.status("Using Kubernetes at: " + client.getMasterUrl());
        log.status("Creating kubernetes resources inside namespace: " + namespace);
        log.info("if you use OpenShift then type this switch namespaces:     oc project " + namespace);
        log.info("if you use kubernetes then type this to switch namespaces: kubectl namespace " + namespace);

        clearTestResultDirectories(session);

        controller.setNamespace(namespace);
        controller.setThrowExceptionOnError(true);
        controller.setRecreateMode(true);
        controller.setIgnoreRunningOAuthClients(true);

        if (configuration.isCreateNamespaceForTest()) {
            createNamespace(client, controller, session);
        } else {
            String namespaceToUse = configuration.getNamespace();
            checkNamespace(client, controller, session, configuration);
            updateConfigMapStatus(client, session, Constants.RUNNING_STATUS);
            namespace = namespaceToUse;
            controller.setNamespace(namespace);
        }

        List<KubernetesList> kubeConfigs = new LinkedList<>();
        shutdownHook = new ShutdownHook(client, controller, configuration, session, kubeConfigs);
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        try {
            URL configUrl = configuration.getEnvironmentConfigUrl();
            List<String> dependencies = !configuration.getEnvironmentDependencies().isEmpty() ? configuration.getEnvironmentDependencies() : resolver.resolve(session);

            if (configuration.isEnvironmentInitEnabled()) {
                for (String dependency : dependencies) {
                    log.info("Found dependency: " + dependency);
                    loadDependency(log, kubeConfigs, dependency, controller, configuration, namespace);
                }
                OpenShiftClient openShiftClient = controller.getOpenShiftClientOrNull();
                if (configUrl == null) {
                    // lets try find the default configuration generated by the new fabric8-maven-plugin
                    String resourceName = "kubernetes.yml";
                    if (openShiftClient != null &&
                            openShiftClient.supportsOpenShiftAPIGroup(OpenShiftAPIGroups.IMAGE) &&
                            openShiftClient.supportsOpenShiftAPIGroup(OpenShiftAPIGroups.ROUTE)) {
                        resourceName = "openshift.yml";
                    }
                    configUrl = findConfigResource("/META-INF/fabric8/" + resourceName);
                }
                if (configUrl != null) {
                    log.status("Applying kubernetes configuration from: " + configUrl);
                    String configText = readAsString(configUrl);
                    Object dto = null;
                    String configPath = configUrl.getPath();
                    if (configPath.endsWith(".yml") || configPath.endsWith(".yaml")) {
                        dto = loadYaml(configText, KubernetesResource.class);
                    } else {
                        dto = loadJson(configText);
                    }
                    dto = expandTemplate(controller, configuration, log, namespace, configUrl.toString(), dto);
                    KubernetesList kubeList = KubernetesHelper.asKubernetesList(dto);
                    List<HasMetadata> items = kubeList.getItems();

                    kubeConfigs.add(kubeList);
                }

                // Lets also try to load the image stream for the project.
                if (openShiftClient != null &&
                        openShiftClient.supportsOpenShiftAPIGroup(OpenShiftAPIGroups.IMAGE)) {
                    File targetDir = new File(System.getProperty("basedir", ".") +"/target");
                    if( targetDir.exists() && targetDir.isDirectory() ) {
                        File[] files = targetDir.listFiles();
                        if( files!=null ) {
                            for (File file : files) {
                                if( file.getName().endsWith("-is.yml") ) {
                                    loadDependency(log, kubeConfigs, file.toURI().toURL().toString(), controller, configuration, namespace);
                                }
                            }
                        }
                    }
                    //
                }

            }
            if (!configuration.isEnvironmentInitEnabled() || applyConfiguration(client, controller, configuration, session, kubeConfigs)) {
                displaySessionStatus(client, session);
            } else {
                throw new IllegalStateException("Failed to apply kubernetes configuration.");
            }
        } catch (Exception e) {
            try {
                cleanupSession(client, controller, configuration, session, kubeConfigs, Constants.ERROR_STATUS);
            } catch (MultiException me) {
                throw e;
            } finally {
                if (shutdownHook != null) {
                    Runtime.getRuntime().removeShutdownHook(shutdownHook);
                }
            }
            throw new RuntimeException(e);
        }
    }

    private void clearTestResultDirectories(Session session) {
        Files.recursiveDelete(new File(session.getBaseDir(), "target/test-pod-status"));
        Files.recursiveDelete(new File(session.getBaseDir(), "target/test-pod-logs"));
    }

    protected Object expandTemplate(Controller controller, Configuration configuration, Logger log, String namespace, String sourceName, Object dto) {
        if (dto instanceof Template) {
            Template template = (Template) dto;
            KubernetesHelper.setNamespace(template, namespace);
            String parameterNamePrefix = "";
            overrideTemplateParameters(template, configuration.getProperties(), parameterNamePrefix);
            log.status("Applying template in namespace " + namespace);
            controller.installTemplate(template, sourceName);
            dto = controller.processTemplate(template, sourceName);
            if (dto == null) {
                throw new IllegalArgumentException("Failed to process Template!");
            }
        }
        return dto;
    }


    protected void addConfig(List<KubernetesList> kubeConfigs, Object dto, Controller controller, Configuration configuration, Logger log, String namespace, String sourceName) {
        dto = expandTemplate(controller, configuration, log, namespace, sourceName, dto);
        if (dto instanceof KubernetesList) {
            kubeConfigs.add((KubernetesList) dto);
        } else if (dto instanceof HasMetadata) {
            // Wrap it in a KubernetesList
            KubernetesList wrappedItem = new KubernetesListBuilder().withItems((HasMetadata) dto).build();
            kubeConfigs.add(wrappedItem);
        } else {
            throw new IllegalArgumentException("Unsupported object type in " + sourceName + ". Class: " + (dto != null ? dto.getClass().getName() : "null object"));
        }
    }

    public void loadDependency(Logger log, List<KubernetesList> kubeConfigs, String dependency, Controller controller, Configuration configuration, String namespace) throws Exception {
        // lets test if the dependency is a local string
        String baseDir = System.getProperty("basedir", ".");
        String path = baseDir + "/" + dependency;
        File file = new File(path);
        if (file.exists()) {
            loadDependency(log, kubeConfigs, file, controller, configuration, log, namespace);
        } else {
            String text = readAsString(createURL(dependency));
            Object resources;
            if (text.trim().startsWith("---") || dependency.endsWith(".yml") || dependency.endsWith(".yaml")) {
                resources = loadYaml(text);
            }  else {
                resources = loadJson(text);
            }
            addConfig(kubeConfigs, resources, controller, configuration, log, namespace, dependency);
        }
    }

    protected URL createURL(final String dependency) throws Exception {
        return URLs.doWithMavenURLHandlerFactory(new Callable<URL>() {
            @Override
            public URL call() throws Exception {
                return new URL(dependency);
            }
        });
    }

    protected void loadDependency(Logger log, List<KubernetesList> kubeConfigs, File file, Controller controller, Configuration configuration, Logger logger, String namespace) throws IOException {
        if (file.isFile()) {
            log.info("Loading file " + file);
            Object content;
            if (file.getName().endsWith(".yaml") || file.getName().endsWith(".yml")) {
                content = loadYaml(file);
            } else {
                content = loadJson(file);
            }
            addConfig(kubeConfigs, content, controller, configuration, log, namespace, file.getPath());
        } else {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    String name = child.getName().toLowerCase();
                    if (name.endsWith(".json") || name.endsWith(".yaml") || name.endsWith(".yml")) {
                        loadDependency(log, kubeConfigs, child, controller, configuration, log, namespace);
                    }
                }
            }
        }
    }

    public void stop(@Observes Stop event, KubernetesClient client, Controller controller, Configuration configuration, List<KubernetesList> kubeConfigs) throws Exception {
        try {
            Session session = event.getSession();
            cleanupSession(client, controller, configuration, session, kubeConfigs, Util.getSessionStatus(session));
        } finally {
            if (shutdownHook != null) {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            }
        }
    }

    private boolean applyConfiguration(KubernetesClient client, Controller controller, Configuration configuration, Session session, List<KubernetesList> kubeConfigs) throws Exception {
        Logger log = session.getLogger();
        Map<Integer, Callable<Boolean>> conditions = new TreeMap<>();
        Callable<Boolean> sessionPodsReady = new SessionPodsAreReady(client, session);
        Callable<Boolean> servicesReady = new SessionServicesAreReady(client, session, configuration);

        Set<HasMetadata> entities = new TreeSet<>(new HasMetadataComparator());
        for (KubernetesList c : kubeConfigs) {
            entities.addAll(enhance(session, configuration ,c).getItems());
        }

        if (containsImageStreamResources(entities)) {
            // no need to use a local image registry
            // as we are using OpenShift and
        } else {
            String registry = getLocalDockerRegistry();
            if (Strings.isNotBlank(registry)) {
                log.status("Adapting resources to pull images from registry: " + registry);
                addRegistryToImageNameIfNotPresent(entities, registry);
            } else {
                log.status("No local fabric8 docker registry found");
            }
        }

        List<Object> items = new ArrayList<>();
        items.addAll(entities);
        //Ensure services are processed first.
        Collections.sort(items, new Comparator<Object>() {
            @Override
            public int compare(Object left, Object right) {
                if (left instanceof Service) {
                    return -1;
                } else if (right instanceof Service) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });

        boolean isOpenshift = client.isAdaptable(OpenShiftClient.class);
        String namespace = session.getNamespace();
        String routeDomain = null;
        if (Strings.isNotBlank(configuration.getKubernetesDomain())) {
            routeDomain = configuration.getKubernetesDomain();
        }

        preprocessEnvironment(client, controller, configuration, session);

        Set<HasMetadata> extraEntities = new TreeSet<>(new HasMetadataComparator());
        for (Object entity : items) {
            if (entity instanceof Pod) {
                Pod pod = (Pod) entity;
                log.status("Applying pod:" + getName(pod));
                Set<Secret> secrets = generateSecrets(client, session, pod.getMetadata());
                String serviceAccountName = pod.getSpec().getServiceAccountName();
                if (Strings.isNotBlank(serviceAccountName)) {
                    generateServiceAccount(client, session, secrets, serviceAccountName);
                }
                controller.applyPod(pod, session.getId());
                conditions.put(1, sessionPodsReady);
            } else if (entity instanceof Service) {
                Service service = (Service) entity;
                String serviceName = getName(service);
                log.status("Applying service:" + serviceName);
                controller.applyService(service, session.getId());
                conditions.put(2, servicesReady);

                if (isOpenshift) {
                    Route route = Routes.createRouteForService(routeDomain, namespace, service, log);
                    if (route != null) {
                        log.status("Applying route for:" + serviceName);
                        controller.applyRoute(route, "route for " + serviceName);
                        extraEntities.add(route);
                    }
                }
            } else if (entity instanceof ReplicationController) {
                ReplicationController replicationController = (ReplicationController) entity;
                log.status("Applying replication controller:" + getName(replicationController));
                Set<Secret> secrets = generateSecrets(client, session, replicationController.getSpec().getTemplate().getMetadata());
                String serviceAccountName = replicationController.getSpec().getTemplate().getSpec().getServiceAccountName();
                if (Strings.isNotBlank(serviceAccountName)) {
                    generateServiceAccount(client, session, secrets, serviceAccountName);
                }
                controller.applyReplicationController(replicationController, session.getId());
                conditions.put(1, sessionPodsReady);
            } else if (entity instanceof ReplicaSet || entity instanceof Deployment || entity instanceof DeploymentConfig) {
                log.status("Applying " + entity.getClass().getSimpleName() + ".");
                controller.apply(entity, session.getId());
                conditions.put(1, sessionPodsReady);
            } else if (entity instanceof OAuthClient) {
                OAuthClient oc = (OAuthClient) entity;
                // these are global so lets create a custom one for the new namespace
                ObjectMeta metadata = KubernetesHelper.getOrCreateMetadata(oc);
                String name = metadata.getName();
                if (isOpenshift) {
                    OpenShiftClient openShiftClient = client.adapt(OpenShiftClient.class);
                    OAuthClient current = openShiftClient.oAuthClients().withName(name).get();
                    boolean create = false;
                    if (current == null) {
                        current = oc;
                        create = true;
                    }
                    boolean updated = false;
                    // lets add a new redirect entry
                    List<String> redirectURIs = current.getRedirectURIs();
                    String namespaceSuffix = "-" + namespace;
                    String redirectUri = "http://" + name + namespaceSuffix;
                    if (Strings.isNotBlank(routeDomain)) {
                        redirectUri += "." + Strings.stripPrefix(routeDomain, ".");
                    }
                    if (!redirectURIs.contains(redirectUri)) {
                        redirectURIs.add(redirectUri);
                        updated = true;
                    }
                    current.setRedirectURIs(redirectURIs);
                    log.status("Applying OAuthClient:" + name);
                    controller.setSupportOAuthClients(true);
                    if (create) {
                        openShiftClient.oAuthClients().create(current);
                    } else {
                        if (updated) {
                            // TODO this should work!
                            // openShiftClient.oAuthClients().withName(name).replace(current);
                            openShiftClient.oAuthClients().withName(name).delete();
                            current.getMetadata().setResourceVersion(null);
                            openShiftClient.oAuthClients().create(current);
                        }
                    }
                }
            } else if (entity instanceof HasMetadata) {
                log.status("Applying " + entity.getClass().getSimpleName() + ":" + KubernetesHelper.getName((HasMetadata) entity));
                controller.apply(entity, session.getId());
            } else if (entity != null) {
                log.status("Applying " + entity.getClass().getSimpleName() + ".");
                controller.apply(entity, session.getId());
            }
        }
        entities.addAll(extraEntities);


        //Wait until conditions are meet.
        if (!conditions.isEmpty()) {
            Callable<Boolean> compositeCondition = new CompositeCondition(conditions.values());
            WaitStrategy waitStrategy = new WaitStrategy(compositeCondition, configuration.getWaitTimeout(), configuration.getWaitPollInterval());
            if (!waitStrategy.await()) {
                log.error("Timed out waiting for pods/services!");
                return false;
            } else {
                log.status("All pods/services are currently 'running'!");
            }
        } else {
            log.warn("No pods/services/replication controllers defined in the configuration!");
        }

        return true;
    }

    private boolean containsImageStreamResources(Iterable<HasMetadata> entities) {
        if (entities != null) {
            for (HasMetadata entity : entities) {
                if (entity instanceof ImageStream) {
                    return true;
                }
            }
        }
        return false;
    }

    protected void preprocessEnvironment(KubernetesClient client, Controller controller, Configuration configuration, Session session) {
        if (configuration.isUseGoFabric8()) {
            // lets invoke gofabric8 to configure the security and secrets
            Logger logger = session.getLogger();
            Commands.assertCommand(logger, "oc", "project", session.getNamespace());
            Commands.assertCommand(logger, "gofabric8", "deploy", "-y", "--console=false", "--templates=false");
            Commands.assertCommand(logger, "gofabric8", "secrets", "-y");
        }
    }


    private void generateServiceAccount(KubernetesClient client, Session session, Set<Secret> secrets, String serviceAccountName) {
        List<ObjectReference> secretRefs = new ArrayList<>();
        for (Secret secret : secrets) {
            secretRefs.add(
                    new ObjectReferenceBuilder()
                            .withNamespace(session.getNamespace())
                            .withName(KubernetesHelper.getName(secret))
                            .build()
            );
        }


        SecurityContextConstraints securityContextConstraints = client.securityContextConstraints().withName(session.getNamespace()).get();
        if (securityContextConstraints == null) {
            client.securityContextConstraints().createNew()
                    .withNewMetadata()
                    .withName(session.getNamespace())
                    .endMetadata()
                    .withAllowHostDirVolumePlugin(true)
                    .withAllowPrivilegedContainer(true)
                    .withNewRunAsUser()
                    .withType("RunAsAny")
                    .endRunAsUser()
                    .withNewSeLinuxContext()
                    .withType("RunAsAny")
                    .endSeLinuxContext()
                    .withUsers("system:serviceaccount:" + session.getNamespace() + ":" + serviceAccountName)
                    .done();
        }

        ServiceAccount serviceAccount = client.serviceAccounts()
                .inNamespace(session.getNamespace())
                .withName(serviceAccountName)
                .get();

        if (serviceAccount == null) {
            client.serviceAccounts().inNamespace(session.getNamespace()).createNew()
                    .withNewMetadata()
                    .withName(serviceAccountName)
                    .endMetadata()
                    .withSecrets(secretRefs)
                    .done();
        } else {
            client.serviceAccounts().inNamespace(session.getNamespace())
                    .withName(serviceAccountName)
                    .replace(new ServiceAccountBuilder(serviceAccount)
                            .withNewMetadata()
                            .withName(serviceAccountName)
                            .endMetadata()
                            .addToSecrets(secretRefs.toArray(new ObjectReference[secretRefs.size()]))
                            .build());
        }
    }

    private Set<Secret> generateSecrets(KubernetesClient client, Session session, ObjectMeta meta) {
        Set<Secret> secrets = new HashSet<>();
        Map<String, String> annotations = meta.getAnnotations();
        if (annotations != null && !annotations.isEmpty()) {
            for (Map.Entry<String, String> entry : annotations.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (SecretKeys.isSecretKey(key)) {
                    SecretKeys keyType = SecretKeys.fromValue(key);
                    for (String name : Secrets.getNames(value)) {
                        Map<String, String> data = new HashMap<>();

                        Secret secret = null;
                        try {
                            secret = client.secrets().inNamespace(session.getNamespace()).withName(name).get();
                        } catch (Exception e) {
                            // ignore - probably doesn't exist
                        }

                        if (secret == null) {
                            for (String c : Secrets.getContents(value, name)) {
                                data.put(c, keyType.generate());
                            }

                            secret = client.secrets().inNamespace(session.getNamespace()).createNew()
                                    .withNewMetadata()
                                    .withName(name)
                                    .endMetadata()
                                    .withData(data)
                                    .done();
                            secrets.add(secret);
                        }
                    }
                }
            }
        }
        return secrets;
    }

    private KubernetesList enhance(final Session session, Configuration configuration, KubernetesList kubernetesList) {
        if (configuration == null || configuration.getProperties() == null || !configuration.getProperties().containsKey(Constants.KUBERNETES_MODEL_PROCESSOR_CLASS)) {
            return kubernetesList;
        }

        String processorClassName = configuration.getProperties().get(Constants.KUBERNETES_MODEL_PROCESSOR_CLASS);
        try {
            final Object instance = SessionListener.class.getClassLoader().loadClass(processorClassName).newInstance();

            KubernetesListBuilder builder = new KubernetesListBuilder(kubernetesList);

            ((Visitable) builder).accept(new Visitor() {
                @Override
                public void visit(Object o) {
                    for (Method m : findMethods(instance, o.getClass())) {
                        Named named = m.getAnnotation(Named.class);
                        if (named != null && !Strings.isNullOrBlank(named.value())) {
                            String objectName = o instanceof ObjectMeta ? getName((ObjectMeta) o) : getName((HasMetadata) o);
                            //If a name has been explicitly specified check if there is a match
                            if (!named.value().equals(objectName)) {
                                session.getLogger().warn("Named method:" + m.getName() + " with name:" + named.value() + " doesn't match: " + objectName + ", ignoring");
                                return;
                            }
                        }
                        try {
                            m.invoke(instance, o);
                        } catch (IllegalAccessException e) {

                        } catch (InvocationTargetException e) {
                            session.getLogger().error("Error invoking visitor method:" + m.getName() + " on:" + instance + "with argument:" + o);
                        }
                    }
                }
            });

            return builder.build();
        } catch (Exception e) {
            session.getLogger().warn("Failed to load processor class:" + processorClassName + ". Ignoring");
            return kubernetesList;
        }
    }

    private static Set<Method> findMethods(Object instance, Class argumentType) {
        Set<Method> result = new LinkedHashSet<>();

        for (Method m : instance.getClass().getDeclaredMethods()) {
            if (m.getParameterTypes().length == 1 && m.getParameterTypes()[0].isAssignableFrom(argumentType)) {
                result.add(m);
            }
        }
        return result;
    }

    private String getLocalDockerRegistry() {
        if (Strings.isNotBlank(System.getenv(Constants.FABRIC8_DOCKER_REGISTRY_SERVICE_HOST))){
            return System.getenv(Constants.FABRIC8_DOCKER_REGISTRY_SERVICE_HOST) + ":" + System.getenv(Constants.FABRIC8_DOCKER_REGISTRY_SERVICE_PORT);
        }
        return null;

    }

    public void addRegistryToImageNameIfNotPresent(Iterable<HasMetadata> items, String registry) throws Exception {
        if (items != null) {
            for (HasMetadata item : items) {
                if (item instanceof KubernetesList) {
                    KubernetesList list = (KubernetesList) item;
                    addRegistryToImageNameIfNotPresent(list.getItems(), registry);
                } else if (item instanceof Template) {
                    Template template = (Template) item;
                    addRegistryToImageNameIfNotPresent(template.getObjects(), registry);
                } else if (item instanceof Pod) {
                    List<Container> containers = ((Pod) item).getSpec().getContainers();
                    prefixRegistryIfNotPresent(containers, registry);

                } else if (item instanceof ReplicationController) {
                    List<Container> containers = ((ReplicationController) item).getSpec().getTemplate().getSpec().getContainers();
                    prefixRegistryIfNotPresent(containers, registry);

                } else if (item instanceof ReplicaSet) {
                    List<Container> containers = ((ReplicaSet) item).getSpec().getTemplate().getSpec().getContainers();
                    prefixRegistryIfNotPresent(containers, registry);

                } else if (item instanceof DeploymentConfig) {
                    List<Container> containers = ((DeploymentConfig) item).getSpec().getTemplate().getSpec().getContainers();
                    prefixRegistryIfNotPresent(containers, registry);

                } else if (item instanceof Deployment) {
                    List<Container> containers = ((Deployment) item).getSpec().getTemplate().getSpec().getContainers();
                    prefixRegistryIfNotPresent(containers, registry);
                }

            }
        }
    }

    private void prefixRegistryIfNotPresent(List<Container> containers, String registry) {
        for (Container container : containers) {
            if (!hasRegistry(container.getImage())){
                container.setImage(registry+"/"+container.getImage());
            }
        }
    }

    /**
     * Checks to see if there's a registry name already provided in the image name
     *
     * Code influenced from <a href="https://github.com/rhuss/docker-maven-plugin/blob/master/src/main/java/org/jolokia/docker/maven/util/ImageName.java">docker-maven-plugin</a>
     * @param imageName
     * @return true if the image name contains a registry
     */
    public static boolean hasRegistry(String imageName) {
        if (imageName == null) {
            throw new NullPointerException("Image name must not be null");
        }
        Pattern tagPattern = Pattern.compile("^(.+?)(?::([^:/]+))?$");
        Matcher matcher = tagPattern.matcher(imageName);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(imageName + " is not a proper image name ([registry/][repo][:port]");
        }

        String rest = matcher.group(1);
        String[] parts = rest.split("\\s*/\\s*");
        String part = parts[0];

        return part.contains(".") || part.contains(":");
    }
}

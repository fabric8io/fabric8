/**
 *  Copyright 2005-2015 Red Hat, Inc.
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
package io.fabric8.forge.rest.main;

import io.fabric8.forge.rest.dto.ExecutionRequest;
import io.fabric8.forge.rest.dto.ExecutionResult;
import io.fabric8.forge.rest.hooks.CommandCompletePostProcessor;
import io.fabric8.forge.rest.ui.RestUIContext;
import io.fabric8.kubernetes.api.Controller;
import io.fabric8.kubernetes.api.KubernetesClient;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.ServiceNames;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.repo.git.CreateRepositoryDTO;
import io.fabric8.repo.git.CreateWebhookDTO;
import io.fabric8.repo.git.GitRepoClient;
import io.fabric8.repo.git.RepositoryDTO;
import io.fabric8.repo.git.WebHookDTO;
import io.fabric8.repo.git.WebhookConfig;
import io.fabric8.utils.Files;
import io.fabric8.utils.URLUtils;
import io.fabric8.utils.cxf.TrustEverythingSSLTrustManager;
import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.PushResult;
import org.jboss.forge.addon.ui.controller.CommandController;
import org.jboss.forge.furnace.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotAuthorizedException;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static io.fabric8.utils.cxf.JsonHelper.toJson;

/**
 * For new projects; lets git add, git commit, git push otherwise lets git add/commit/push any new/udpated changes
 */
// TODO we should try add this into the ConfigureDevOpsStep.execute() block instead!
public class GitCommandCompletePostProcessor implements CommandCompletePostProcessor {
    private static final transient Logger LOG = LoggerFactory.getLogger(GitCommandCompletePostProcessor.class);
    public static final String PROJECT_NEW_COMMAND = "project-new";
    public static final String TARGET_LOCATION_PROPERTY = "targetLocation";
    public static final String DEFAULT_JENKINS_SEED_JOB = "seed";
    private final KubernetesClient kubernetes;
    private final GitUserHelper gitUserHelper;
    private final ProjectFileSystem projectFileSystem;
    private final String jenkinsSeedJob;
    private final boolean createOpenShiftBuildResources;

    @Inject
    public GitCommandCompletePostProcessor(KubernetesClient kubernetes,
                                           GitUserHelper gitUserHelper,
                                           ProjectFileSystem projectFileSystem,
                                           @ConfigProperty(name = "JENKINS_SEED_JOB", defaultValue = DEFAULT_JENKINS_SEED_JOB) String jenkinsSeedJob,
                                           @ConfigProperty(name = "OPENSHIFT_CREATE_BUILD_ON_PROJECT_CREATE", defaultValue = "false") boolean createOpenShiftBuildResources) {
        this.kubernetes = kubernetes;
        this.gitUserHelper = gitUserHelper;
        this.projectFileSystem = projectFileSystem;
        this.jenkinsSeedJob = jenkinsSeedJob;
        this.createOpenShiftBuildResources = createOpenShiftBuildResources;
    }

    @Override
    public UserDetails preprocessRequest(String name, ExecutionRequest executionRequest, HttpServletRequest request) {
        UserDetails userDetails = gitUserHelper.createUserDetails(request);
        if (Strings.isNullOrEmpty(userDetails.getUser()) || Strings.isNullOrEmpty(userDetails.getUser())) {
            throw new NotAuthorizedException("You must authenticate to be able to perform this command");
        }

        if (Objects.equals(name, PROJECT_NEW_COMMAND)) {
            List<Map<String, String>> inputList = executionRequest.getInputList();
            if (inputList != null) {
                Map<String, String> page1 = inputList.get(0);
                if (page1 != null) {
                    if (page1.containsKey(TARGET_LOCATION_PROPERTY)) {
                        page1.put(TARGET_LOCATION_PROPERTY, projectFileSystem.getUserProjectFolderLocation(userDetails));
                    }
                }
            }
        }
        return userDetails;
    }


    @Override
    public void firePostCompleteActions(String name, ExecutionRequest executionRequest, RestUIContext context, CommandController controller, ExecutionResult results, HttpServletRequest request) {
        UserDetails userDetails = gitUserHelper.createUserDetails(request);

        String user = userDetails.getUser();
        String password = userDetails.getPassword();
        String authorEmail = userDetails.getEmail();
        String address = userDetails.getAddress();
        String branch = userDetails.getBranch();
        String origin = projectFileSystem.getRemote();

        try {
            disableSslCertificateChecks();

            CredentialsProvider credentials = userDetails.createCredentialsProvider();
            PersonIdent personIdent = new PersonIdent(user, authorEmail);

            if (name.equals(PROJECT_NEW_COMMAND)) {
                String targetLocation = null;
                String named = null;
                List<Map<String, String>> inputList = executionRequest.getInputList();

                for (Map<String, String> map : inputList) {
                    if (Strings.isNullOrEmpty(targetLocation)) {
                        targetLocation = map.get("targetLocation");
                    }
                    if (Strings.isNullOrEmpty(named)) {
                        named = map.get("named");
                    }
                }
                if (Strings.isNullOrEmpty(targetLocation)) {
                    LOG.warn("No targetLocation could be found!");
                } else if (Strings.isNullOrEmpty(named)) {
                    LOG.warn("No named could be found!");
                } else {
                    File basedir = new File(targetLocation, named);
                    if (!basedir.isDirectory() || !basedir.exists()) {
                        LOG.warn("Generated project folder does not exist: " + basedir.getAbsolutePath());
                    } else {
                        InitCommand initCommand = Git.init();
                        initCommand.setDirectory(basedir);
                        Git git = initCommand.call();
                        LOG.info("Initialised an empty git configuration repo at {}", basedir.getAbsolutePath());

                        // lets create the repository
                        GitRepoClient repoClient = userDetails.createRepoClient();
                        CreateRepositoryDTO createRepository = new CreateRepositoryDTO();
                        createRepository.setName(named);

                        String fullName = null;
                        RepositoryDTO repository = repoClient.createRepository(createRepository);
                        if (repository != null) {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Got repository: " + toJson(repository));
                            }
                            fullName = repository.getFullName();
                        }
                        if (Strings.isNullOrEmpty(fullName)) {
                            fullName = user + "/" + named;
                        }
                        String htmlUrl = address + user + "/" + named;
                        String remoteUrl = address + user + "/" + named + ".git";
                        //results.appendOut("Created git repository " + fullName + " at: " + htmlUrl);

                        results.setOutputProperty("fullName", fullName);
                        results.setOutputProperty("cloneUrl", remoteUrl);
                        results.setOutputProperty("htmlUrl", htmlUrl);

                        // now lets import the code and publish
                        LOG.info("Using remoteUrl: " + remoteUrl + " and remote name " + origin);
                        configureBranch(git, branch, origin, remoteUrl);

                        createKubernetesResources(user, named, remoteUrl, branch, repoClient, address);

                        String message = createCommitMessage(name, executionRequest);
                        doAddCommitAndPushFiles(git, credentials, personIdent, remoteUrl, branch, origin, message);

                        Map<Object, Object> attributeMap = context.getAttributeMap();
                        Object registerWebHooksValue = attributeMap.get("registerWebHooks");
                        if (registerWebHooksValue instanceof Runnable) {
                            Runnable runnable = (Runnable) registerWebHooksValue;
                            projectFileSystem.invokeLater(runnable, 1000L);
                        }

                        // TODO only need to do this if we have not created a jenkins build...
                        //triggerJenkinsSeedBuild();
                    }
                }
            } else {
                File basedir = context.getInitialSelectionFile();
                String absolutePath = basedir != null ? basedir.getAbsolutePath() : null;
                if (basedir != null) {
                    File gitFolder = new File(basedir, ".git");
                    if (gitFolder.exists() && gitFolder.isDirectory()) {
                        FileRepositoryBuilder builder = new FileRepositoryBuilder();
                        Repository repository = builder.setGitDir(gitFolder)
                                .readEnvironment() // scan environment GIT_* variables
                                .findGitDir() // scan up the file system tree
                                .build();

                        Git git = new Git(repository);
                        String remoteUrl = getRemoteURL(git, branch);
                        if (origin == null) {
                            LOG.warn("Could not find remote git URL for folder " + absolutePath);
                        } else {
                            String message = createCommitMessage(name, executionRequest);
                            doAddCommitAndPushFiles(git, credentials, personIdent, remoteUrl, branch, origin, message);
                        }
                    }
                }
            }
        } catch (Exception e) {
            handleException(e);
        }
    }

    /**
     * We've created a project so lets try trigger a jenkins seed build
     */
    protected void triggerJenkinsSeedBuild() {
        if (!Strings.isNullOrEmpty(jenkinsSeedJob)) {
            String address = kubernetes.getServiceURL(ServiceNames.JENKINS, kubernetes.getNamespace(), "http", false);
            if (!Strings.isNullOrEmpty(address)) {
                String jobUrl = URLUtils.pathJoin(address, "/job/", jenkinsSeedJob, "/build");

                LOG.info("Attempting to trigger the jenkins seed build on: " + jobUrl);

                String json = "{}";
                HttpURLConnection connection = null;
                try {
                    URL url = new URL(jobUrl);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setDoOutput(true);

                    OutputStreamWriter out = new OutputStreamWriter(
                            connection.getOutputStream());
                    out.write(json);

                    out.close();
                    int status = connection.getResponseCode();
                    String message = connection.getResponseMessage();
                    LOG.info("Got response code from Jenkins: " + status + " message: " + message);
                    if (status != 200) {
                        LOG.error("Failed to trigger job " + jenkinsSeedJob + " on " + jobUrl + ". Status: " + status + " message: " + message);
                    }
                } catch (Exception e) {
                    LOG.error("Failed to trigger jenkins on " + jobUrl + ". " + e, e);
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }
        }
    }

    protected static void disableSslCertificateChecks() {
        LOG.info("Trusting all SSL certificates");

        try {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new TrustManager[]{new TrustEverythingSSLTrustManager()}, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
            // bypass host name check, too.
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                public boolean verify(String s, SSLSession sslSession) {
                    return true;
                }
            });
        } catch (NoSuchAlgorithmException e) {
            LOG.warn("Failed to bypass certificate check", e);
        } catch (KeyManagementException e) {
            LOG.warn("Failed to bypass certificate check", e);
        }
    }

    /**
     * Lets generate a commit message with the command name and all the parameters we specify
     */
    protected String createCommitMessage(String name, ExecutionRequest executionRequest) {
        StringBuilder builder = new StringBuilder(name);
        List<Map<String, String>> inputList = executionRequest.getInputList();
        for (Map<String, String> map : inputList) {
            Set<Map.Entry<String, String>> entries = map.entrySet();
            for (Map.Entry<String, String> entry : entries) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (!Strings.isNullOrEmpty(value) && !value.equals("0") && !value.toLowerCase().equals("false")) {
                    builder.append(" --");
                    builder.append(key);
                    builder.append("=");
                    builder.append(value);
                }
            }
        }
        return builder.toString();
    }

    /**
     * Lets create an ImageRegistry, BuildConfig and DeploymentConfig for the new project
     */
    protected void createKubernetesResources(String user, String buildName, String remote, String branch, GitRepoClient repoClient, String address) throws Exception {
        String imageTag = "test";
        String secret = "secret101";
        String builderImage = "fabric8/java-main";
        String osapiVersion = "v1";
        String namespace = kubernetes.getNamespace();
        if (Strings.isNullOrEmpty(namespace)) {
            namespace = KubernetesClient.defaultNamespace();
            kubernetes.setNamespace(namespace);
        }
        String gitServiceName = "gogs";


        // TODO we should replace the remote with the actual service IP address???

        String gitAddress = getServiceAddress(gitServiceName, namespace);
        if (gitAddress == null) {
            LOG.warn("Could not find service " + gitServiceName + " for namespace " + namespace);
            gitAddress = address;
        }

        String json = "\n" +
                "{\n" +
                "   \"annotations\":{\n" +
                "      \"description\":\"This is an end to end example of a Continuous Delivery pipeline running on OpenShift v3\"\n" +
                "   },\n" +
                "   \"apiVersion\":\"" + osapiVersion + "\",\n" +
                "   \"kind\":\"List\",\n" +
                "   \"items\":[\n" +
                "      {\n" +
                "         \"apiVersion\":\"" + osapiVersion + "\",\n" +
                "         \"kind\":\"ImageStream\",\n" +
                "         \"metadata\":{\n" +
                "            \"labels\":{\n" +
                "               \"name\":\"" + buildName + "\",\n" +
                "               \"user\":\"" + user + "\"\n" +
                "            },\n" +
                "            \"name\":\"" + buildName + "\"\n" +
                "         }\n" +
                "      },\n" +
                "      {\n" +
                "         \"apiVersion\":\"" + osapiVersion + "\",\n" +
                "         \"kind\":\"BuildConfig\",\n" +
                "         \"metadata\":{\n" +
                "            \"labels\":{\n" +
                "               \"name\":\"" + buildName + "\",\n" +
                "               \"user\":\"" + user + "\"\n" +
                "            },\n" +
                "            \"name\":\"" + buildName + "\"\n" +
                "         },\n" +
                "         \"parameters\":{\n" +
                "            \"output\":{\n" +
                "               \"to\":{\n" +
                "                  \"name\":\"" + buildName + "\"\n" +
                "               },\n" +
                "               \"tag\":\"test\"\n" +
                "            },\n" +
                "            \"source\":{\n" +
                "               \"git\":{\n" +
                "                  \"uri\":\"" + gitAddress + "/" + user + "/" + buildName + ".git\"\n" +
                "               },\n" +
                "               \"type\":\"Git\"\n" +
                "            },\n" +
                "            \"strategy\":{\n" +
                "               \"stiStrategy\":{\n" +
                "                  \"builderImage\":\"" + builderImage + "\",\n" +
                "                  \"image\":\"" + builderImage + "\"\n" +
                "               },\n" +
                "               \"type\":\"STI\"\n" +
                "            }\n" +
                "         },\n" +
                "         \"triggers\":[\n" +
                "            {\n" +
                "               \"github\":{\n" +
                "                  \"secret\":\"" + secret + "\"\n" +
                "               },\n" +
                "               \"type\":\"github\"\n" +
                "            },\n" +
                "            {\n" +
                "               \"generic\":{\n" +
                "                  \"secret\":\"" + secret + "\"\n" +
                "               },\n" +
                "               \"type\":\"generic\"\n" +
                "            }\n" +
                "         ]\n" +
                "      },\n" +
                "      {\n" +
                "         \"apiVersion\":\"" + osapiVersion + "\",\n" +
                "         \"kind\":\"DeploymentConfig\",\n" +
                "         \"metadata\":{\n" +
                "            \"name\":\"" + buildName + "-deploy\"\n" +
                "         },\n" +
                "         \"template\":{\n" +
                "            \"controllerTemplate\":{\n" +
                "               \"podTemplate\":{\n" +
                "                  \"desiredState\":{\n" +
                "                     \"manifest\":{\n" +
                "                        \"containers\":[\n" +
                "                           {\n" +
                "                              \"image\":\"" + buildName + "\",\n" +
                "                              \"name\":\"" + buildName + "\",\n" +
                "                              \"ports\":[\n" +
                "                                 {\n" +
                "                                    \"containerPort\":8778\n" +
                "                                 }\n" +
                "                              ]\n" +
                "                           }\n" +
                "                        ],\n" +
                "                        \"version\":\"" + imageTag + "\"\n" +
                "                     }\n" +
                "                  },\n" +
                "                  \"labels\":{\n" +
                "                     \"name\":\"" + buildName + "\",\n" +
                "                     \"user\":\"" + user + "\"\n" +
                "                  }\n" +
                "               },\n" +
                "               \"replicaSelector\":{\n" +
                "                  \"name\":\"" + buildName + "\",\n" +
                "                  \"user\":\"" + user + "\"\n" +
                "               },\n" +
                "               \"replicas\":1\n" +
                "            },\n" +
                "            \"strategy\":{\n" +
                "               \"type\":\"Recreate\"\n" +
                "            }\n" +
                "         },\n" +
                "         \"triggers\":[\n" +
                "            {\n" +
                "               \"type\":\"ImageChange\",\n" +
                "               \"imageChangeParams\":{\n" +
                "                  \"automatic\":true,\n" +
                "                  \"containerNames\":[\n" +
                "                     \"" + buildName + "\"\n" +
                "                  ],\n" +
                "                  \"from\":{\n" +
                "                     \"name\":\"" + buildName + "\"\n" +
                "                  },\n" +
                "                  \"tag\":\"" + imageTag + "\"\n" +
                "               }\n" +
                "            }\n" +
                "         ]\n" +
                "      }" +
                "   ]\n" +
                "}";

        if (createOpenShiftBuildResources) {
            Controller controller = new Controller(kubernetes);
            controller.applyJson(json);


            String type = "generic";

            // TODO due to https://github.com/openshift/origin/issues/1317 we can't use the direct kube REST API
            // so we need to use a workaround using the fabric8 console service's proxy which hides the payload for us
            String webhookUrl = null;
            String kubeAddress = null;
            boolean appendNamespaceQuery = true;

            // lets try the cdelivery first
            if (kubeAddress == null) {
                try {
                    kubeAddress = kubernetes.getServiceURL(ServiceNames.CDELIVERY_API, namespace, "http", false);
                    webhookUrl = URLUtils.pathJoin(kubeAddress, "buildConfigHooks", namespace, buildName);
                    appendNamespaceQuery = false;
                } catch (Exception e) {
                    LOG.warn("failed to find cdelivery service URL: " + e, e);
                }
            }
/*
        if (kubeAddress == null) {
            try {
                kubeAddress = kubernetes.getServiceURL("fabric8", namespace, "http", false);
                webhookUrl = URLUtils.pathJoin(kubeAddress, "oapi", KubernetesHelper.defaultOsApiVersion, "buildConfigHooks", buildName, secret, type);
            } catch (Exception e) {
                LOG.warn("failed to find fabric8 console service URL: " + e, e);
            }
        }
*/
            if (kubeAddress == null) {
                kubeAddress = kubernetes.getAddress();
            }
            String namespacePath = "";
            if (appendNamespaceQuery && !Strings.isNullOrEmpty(namespace)) {
                namespacePath = "/namespace=" + namespace;
            }

            if (webhookUrl == null) {
                webhookUrl = URLUtils.pathJoin(kubeAddress, "oapi", KubernetesHelper.defaultOsApiVersion + namespacePath, "buildConfigHooks", buildName, secret, type);
            }

            LOG.info("creating a web hook at: " + webhookUrl);
            try {
                CreateWebhookDTO createWebhook = new CreateWebhookDTO();
                createWebhook.setType("gogs");
                WebhookConfig config = createWebhook.getConfig();
                config.setUrl(webhookUrl);
                config.setSecret(secret);
                WebHookDTO webhook = repoClient.createWebhook(user, buildName, createWebhook);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Got web hook: " + toJson(webhook));
                }
            } catch (Exception e) {
                LOG.warn("Failed to create web hook in git repo: " + e, e);
            }
        }



/*        Map<String,String> labels = new LinkedHashMap<>();
        labels.put("name", buildName);
        labels.put("user", user);

        Map<String,String> to = new LinkedHashMap<>();
        to.put("name", buildName);

        ImageStream imageRepository = new ImageStream();
        imageRepository.setKind("ImageStream");
        imageRepository.setApiVersion(osapiVersion);
        imageRepository.setName(buildName);
        imageRepository.setLabels(labels);
        handleKubernetesResourceCreation(imageRepository.getKind(), imageRepository, kubernetes.createImageStream(imageRepository));

        BuildConfig buildConfig = new BuildConfig();
        buildConfig.setKind("BuildConfig");
        buildConfig.setApiVersion(osapiVersion);
        buildConfig.setName(buildName);
        buildConfig.setLabels(labels);

        BuildOutput output = new BuildOutput();
        // TODO should be to: { labels }
        //output.setRegistry(buildName);
        output.getAdditionalProperties().put("to", to);
        output.setImageTag(imageTag);

        BuildSource source = new BuildSource();
        source.setType("Git");
        GitBuildSource git = new GitBuildSource();
        git.setUri(remote);
        source.setGit(git);

        BuildStrategy strategy = new BuildStrategy();
        strategy.setType("STI");
        STIBuildStrategy stiStrategy = new STIBuildStrategy();
        stiStrategy.setImage(builderImage);
        // TODO
        //stiStrategy.setBuilderImage(builderImage);
        stiStrategy.getAdditionalProperties().put("builderImage", builderImage);
        strategy.setStiStrategy(stiStrategy);

        BuildParameters parameters = new BuildParameters();
        parameters.setOutput(output);
        parameters.setSource(source);
        parameters.setStrategy(strategy);
        buildConfig.setParameters(parameters);

        BuildTriggerPolicy github = new BuildTriggerPolicy();
        github.setType("github");
        WebHookTrigger githubTrigger = new WebHookTrigger();
        githubTrigger.setSecret(secret);
        github.setGithub(githubTrigger);

        BuildTriggerPolicy generic = new BuildTriggerPolicy();
        generic.setType("generic");
        WebHookTrigger genericTrigger = new WebHookTrigger();
        genericTrigger.setSecret(secret);
        generic.setGeneric(genericTrigger);

        List<BuildTriggerPolicy> triggers = new ArrayList<>();
        triggers.add(github);
        triggers.add(generic);
        buildConfig.setTriggers(triggers);
        
        handleKubernetesResourceCreation(buildConfig.getKind(), buildConfig, kubernetes.createBuildConfig(buildConfig));*/
    }

    protected String getServiceAddress(String serviceName, String namespace) {
        LOG.info("Looking up service " + serviceName + " for namespace: " + namespace);
        io.fabric8.kubernetes.api.model.Service service = kubernetes.getService(serviceName, namespace);

        String serviceAddress = null;
        if (service != null) {
            String portalIP = service.getSpec().getPortalIP();
            if (!Strings.isNullOrEmpty(portalIP)) {
                List<ServicePort> servicePorts = service.getSpec().getPorts();
                if (servicePorts != null && !servicePorts.isEmpty()) {
                    Integer port = servicePorts.iterator().next().getPort();
                    String prefix = "http://";
                    String postfix = "";
                    if (port != null) {
                        if (port == 443) {
                            prefix = "https://";
                        }

                        if (port != 80) {
                            postfix = ":" + port;
                        }
                    }
                    serviceAddress = prefix + portalIP + postfix;
                }
            }
        }
        return serviceAddress;
    }

    protected void handleKubernetesResourceCreation(String kind, Object entity, String results) {
        LOG.warn("Created " + entity + ". Results: " + results);
    }

    /**
     * Returns the remote git URL for the given branch
     */
    protected String getRemoteURL(Git git, String branch) {
        StoredConfig config = git.getRepository().getConfig();
        return config.getString("branch", branch, "remote");
    }

    protected void configureBranch(Git git, String branch, String origin, String remoteRepository) {
        // lets update the merge config
        if (!Strings.isNullOrEmpty(branch)) {
            StoredConfig config = git.getRepository().getConfig();
            config.setString("branch", branch, "remote", origin);
            config.setString("branch", branch, "merge", "refs/heads/" + branch);

            config.setString("remote", origin, "url", remoteRepository);
            config.setString("remote", origin, "fetch", "+refs/heads/*:refs/remotes/" + origin + "/*");
            try {
                config.save();
            } catch (IOException e) {
                LOG.error("Failed to save the git configuration to " + git.getRepository().getDirectory()
                        + " with branch " + branch + " on " + origin + " remote repo: " + remoteRepository + " due: " + e.getMessage() + ". This exception is ignored.", e);
            }
        }
    }

    private void addFiles(Git git, File... files) throws GitAPIException, IOException {
        File rootDir = GitHelpers.getRootGitDirectory(git);
        for (File file : files) {
            String relativePath = getFilePattern(rootDir, file);
            git.add().addFilepattern(relativePath).call();
        }
    }

    private String getFilePattern(File rootDir, File file) throws IOException {
        String relativePath = Files.getRelativePath(rootDir, file);
        if (relativePath.startsWith(File.separator)) {
            relativePath = relativePath.substring(1);
        }
        return relativePath.replace(File.separatorChar, '/');
    }

    protected void doAddCommitAndPushFiles(Git git, CredentialsProvider credentials, PersonIdent personIdent, String remote, String branch, String origin, String message) throws GitAPIException, IOException {
        git.add().addFilepattern(".").call();
        doCommitAndPush(git, message, credentials, personIdent, remote, branch, origin);
    }

    protected RevCommit doCommitAndPush(Git git, String message, CredentialsProvider credentials, PersonIdent author, String remote, String branch, String origin) throws IOException, GitAPIException {
        CommitCommand commit = git.commit().setAll(true).setMessage(message);
        if (author != null) {
            commit = commit.setAuthor(author);
        }

        RevCommit answer = commit.call();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Committed " + answer.getId() + " " + answer.getFullMessage());
        }

        if (isPushOnCommit()) {
            Iterable<PushResult> results = git.push().setCredentialsProvider(credentials).setRemote(origin).call();
            for (PushResult result : results) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Pushed " + result.getMessages() + " " + result.getURI() + " branch: " + branch + " updates: " + GitHelpers.toString(result.getRemoteUpdates()));
                }
            }
        }
        return answer;
    }

    protected boolean isPushOnCommit() {
        return true;
    }

    protected void handleException(Throwable e) {
        LOG.warn("Caught: " + e, e);
        throw new RuntimeException(e);
    }
}

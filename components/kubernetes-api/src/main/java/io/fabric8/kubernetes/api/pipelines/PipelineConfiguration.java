/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.kubernetes.api.pipelines;


import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.utils.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.IntrospectionException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.fabric8.kubernetes.api.environments.Environments.findSpaceNamespace;

/**
 */
public class PipelineConfiguration {
    private static final transient Logger LOG = LoggerFactory.getLogger(PipelineConfiguration.class);

    /**
     * The name of the ConfigMap which stores the {@link PipelineConfiguration}
     */
    public static final String FABRIC8_PIPELINES = "fabric8-pipelines";

    public static final String CI_BRANCH_PATTERNS = "ci-branch-patterns";
    public static final String CD_BRANCH_PATTERNS = "cd-branch-patterns";
    public static final String ORGANISATION_BRANCH_PATTERNS = "organisation-branch-patterns";
    public static final String JOB_NAME_TO_KIND = "job-name-to-kind";
    public static final String DISABLE_CD_ITESTS = "disable-itests-cd";
    public static final String DISABLE_CI_ITESTS = "disable-itests-ci";
    public static final String USE_DOCKER_SOCKET = "use-docker-socket";

    private Map<String, String> data;
    private Map<String, PipelineKind> jobNameToKindMap = new HashMap<>();
    private List<String> ciBranchPatterns = new ArrayList<>();
    private List<String> cdBranchPatterns = new ArrayList<>();
    private Map<String, List<String>> cdGitHostAndOrganisationToBranchPatterns = new HashMap<>();
    private boolean disableITestsCD;
    private boolean disableITestsCI;
    private Boolean useDockerSocketFlag;
    private String spaceNamespace;
    private Map<String, List<String>> cdGitHostAndOrganisationToBranchPatterns = new HashMap<>();


    public PipelineConfiguration() {
        this.data = new HashMap<>();
    }

    public PipelineConfiguration(Map<String, String> configMapData) {
        this.data = configMapData;
        this.ciBranchPatterns = loadYamlListOfStrings(configMapData, CI_BRANCH_PATTERNS);
        this.cdBranchPatterns = loadYamlListOfStrings(configMapData, CD_BRANCH_PATTERNS);
        this.disableITestsCD = loadYamlBoolean(configMapData, DISABLE_CD_ITESTS, false);
        this.disableITestsCI = loadYamlBoolean(configMapData, DISABLE_CI_ITESTS, false);
        this.useDockerSocketFlag = loadYamlBooleanOptional(configMapData, USE_DOCKER_SOCKET, null);

        Map<Object, Object> orgBranchMap = loadYamlMap(configMapData, ORGANISATION_BRANCH_PATTERNS);
        for (Map.Entry<Object, Object> entry : orgBranchMap.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            if (key instanceof String) {
                String keyText = (String) key;
                List<String> list = null;
                if (value instanceof List) {
                    list = (List<String>) value;
                } else if (value != null) {
                    String valueText = value.toString();
                    list = new ArrayList<>();
                    list.add(valueText);
                }

                if (list != null) {
                    cdGitHostAndOrganisationToBranchPatterns.put(keyText, list);
                } else {
                    LOG.warn("Could not find List for " + ORGANISATION_BRANCH_PATTERNS + " key " + key + " value: " + value);
                }
            }
        }
        Map<Object, Object> jobNameMap = loadYamlMap(configMapData, JOB_NAME_TO_KIND);
        for (Map.Entry<Object, Object> entry : jobNameMap.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            if (key != null && value != null) {
                String keyText = key.toString();
                String valueText = value.toString();
                try {
                    PipelineKind pipelineKind = PipelineKind.valueOf(valueText);
                    jobNameToKindMap.put(keyText, pipelineKind);
                } catch (IllegalArgumentException e) {
                    LOG.warn("Ignoring " + JOB_NAME_TO_KIND + " key " + key + " with value: " + value
                            + ". Values are: " + Arrays.asList(PipelineKind.values()) + ". " + e, e);
                }
            }
        }
    }


    public static PipelineConfiguration createDefault() {
        PipelineConfiguration configuration = new PipelineConfiguration();
        configuration.getCiBranchPatterns().add("PR-.*");
        return configuration;
    }

    /**
     * Parses the git URL string and determines the host and organisation string
     */
    public static String getGitHostOrganisationString(String gitUrl) {
        if (Strings.isNullOrBlank(gitUrl)) {
            return null;
        }
        String prefix = "git@";
        if (gitUrl.startsWith(prefix)) {
            String text = gitUrl.substring(prefix.length());
            int idx = text.indexOf('/');
            if (idx > 0) {
                return text.substring(0, idx).replace(':', '/');
            }
        }
        String schemeSuffix = "://";
        int idx = gitUrl.indexOf(schemeSuffix);
        if (idx > 0) {
            String text = gitUrl.substring(idx + schemeSuffix.length());
            idx = text.indexOf("/");
            if (idx > 0) {
                idx = text.indexOf("/", idx + 1);
                if (idx > 0) {
                    return text.substring(0, idx);
                }
            }
        }
        return null;
    }

    public static PipelineConfiguration loadPipelineConfiguration() {
        try (KubernetesClient kubernetesClient = new DefaultKubernetesClient()) {
            String namespace = findSpaceNamespace(kubernetesClient);
            return loadPipelineConfiguration(kubernetesClient, namespace);
        }
    }


    public static PipelineConfiguration loadPipelineConfiguration(String namespace) {
        try (KubernetesClient kubernetesClient = new DefaultKubernetesClient()) {
            return loadPipelineConfiguration(kubernetesClient, namespace);
        }
    }

    /**
     * Loads the pipeline configuration from the namespace in kubernetes if it is present. Otherwise a default
     * configuration is loaded.
     */
    public static PipelineConfiguration loadPipelineConfiguration(KubernetesClient kubernetesClient, String namespace) {
        ConfigMap configMap = kubernetesClient.configMaps().inNamespace(namespace).withName(FABRIC8_PIPELINES).get();
        PipelineConfiguration configuration;
        if (configMap != null) {
            configuration = getPipelineConfiguration(configMap);
        } else {
            // we may be in the wrong environment so lets try find the environments
            configuration = createDefault();
        }
        configuration.setSpaceNamespace(namespace);
        return configuration;
    }

    /**
     * Saves the {@link PipelineConfiguration} into a {@link ConfigMap} in the given namespace
     */
    public static void savePipelineConfiguration(KubernetesClient kubernetesClient, String namespace, PipelineConfiguration configuration) {
        ConfigMap configMap = configuration.createConfigMap();
        kubernetesClient.configMaps().inNamespace(namespace).withName(FABRIC8_PIPELINES).createOrReplace(configMap);
    }

    @Override
    public String toString() {
        return "PipelineConfiguration{" +
                "jobNameToKindMap=" + jobNameToKindMap +
                ", ciBranchPatterns=" + ciBranchPatterns +
                ", cdBranchPatterns=" + cdBranchPatterns +
                ", cdGitHostAndOrganisationToBranchPatterns=" + cdGitHostAndOrganisationToBranchPatterns +
                ", disableITestsCD=" + disableITestsCD +
                ", disableITestsCI=" + disableITestsCI +
                ", useDockerSocketFlag=" + useDockerSocketFlag +
                '}';
    }

    /**
     * Creates the {@link ConfigMap} for the current configuration
     */
    public ConfigMap createConfigMap() {
        Map<String, String> data = new HashMap<>();
        data.put(JOB_NAME_TO_KIND, asYaml(jobNameToKindMap));
        data.put(CI_BRANCH_PATTERNS, asYaml(ciBranchPatterns));
        data.put(CD_BRANCH_PATTERNS, asYaml(cdBranchPatterns));
        data.put(ORGANISATION_BRANCH_PATTERNS, asYaml(cdGitHostAndOrganisationToBranchPatterns));
        return new ConfigMapBuilder().withNewMetadata().
                withName(FABRIC8_PIPELINES).addToLabels("provider", "fabric8").endMetadata().
                withData(data).build();
    }

    private String asYaml(Object value) {
        if (value != null) {
            try {
                return KubernetesHelper.toYaml(value);
            } catch (IOException e) {
                LOG.warn("Error trying to convert " + value + " to YAML: " + e, e);
            }
        }
        return "";
    }

    public static PipelineConfiguration getPipelineConfiguration(ConfigMap configMap) {
        Map<String, String> data = configMap.getData();
        if (data == null) {
            data = new HashMap<>();
        }
        return new PipelineConfiguration(data);
    }

    private Map<Object, Object> loadYamlMap(Map<String, String> configMapData, String key) {
        String text = configMapData.get(key);
        if (Strings.isNotBlank(text)) {
            try {
                return KubernetesHelper.loadYaml(text, Map.class);
            } catch (IOException e) {
                LOG.warn("Failed to read key " + key + " with text " + text + " due to: " + e, e);
            }
        }
        return Collections.EMPTY_MAP;
    }

    private List<String> loadYamlListOfStrings(Map<String, String> configMapData, String key) {
        List<String> answer = new ArrayList<>();
        String text = configMapData.get(key);
        if (Strings.isNotBlank(text)) {
            try {
                List list = KubernetesHelper.loadYaml(text, List.class);
                for (Object value : list) {
                    if (value instanceof String) {
                        String textValue = (String) value;
                        answer.add(textValue);
                    }
                }
            } catch (IOException e) {
                LOG.warn("Failed to read key " + key + " with text " + text + " due to: " + e, e);
            }
        }
        return answer;
    }

    private Boolean loadYamlBooleanOptional(Map<String, String> configMapData, String key, Boolean defaultValue) {
        String text = configMapData.get(key);
        if (Strings.isNotBlank(text)) {
            return text.equalsIgnoreCase("true");
        }
        return defaultValue;
    }

    private boolean loadYamlBoolean(Map<String, String> configMapData, String key, boolean defaultValue) {
        String text = configMapData.get(key);
        if (Strings.isNotBlank(text)) {
            return text.equalsIgnoreCase("true");
        }
        return defaultValue;
    }

    public Map<String, PipelineKind> getJobNameToKindMap() {
        return jobNameToKindMap;
    }

    public List<String> getCiBranchPatterns() {
        return ciBranchPatterns;
    }

    public List<String> getCdBranchPatterns() {
        return cdBranchPatterns;
    }

    public Boolean getUseDockerSocketFlag() {
        return useDockerSocketFlag;
    }

    public boolean isUseDockerSocket() {
        return useDockerSocketFlag != null && useDockerSocketFlag.booleanValue();
    }

    public void setUseDockerSocketFlag(Boolean useDockerSocket) {
        this.useDockerSocketFlag = useDockerSocket;
    }

    public Map<String, List<String>> getCdGitHostAndOrganisationToBranchPatterns() {
        return cdGitHostAndOrganisationToBranchPatterns;
    }

    public void setJobNameToKindMap(Map<String, PipelineKind> jobNameToKindMap) {
        this.jobNameToKindMap = jobNameToKindMap;
    }

    public void setCiBranchPatterns(List<String> ciBranchPatterns) {
        this.ciBranchPatterns = ciBranchPatterns;
    }

    public void setCdBranchPatterns(List<String> cdBranchPatterns) {
        this.cdBranchPatterns = cdBranchPatterns;
    }

    public void setCdGitHostAndOrganisationToBranchPatterns(Map<String, List<String>> cdGitHostAndOrganisationToBranchPatterns) {
        this.cdGitHostAndOrganisationToBranchPatterns = cdGitHostAndOrganisationToBranchPatterns;
    }

    public boolean isDisableITestsCD() {
        return disableITestsCD;
    }

    public void setDisableITestsCD(boolean disableITestsCD) {
        this.disableITestsCD = disableITestsCD;
    }

    public boolean isDisableITestsCI() {
        return disableITestsCI;
    }

    public void setDisableITestsCI(boolean disableITestsCI) {
        this.disableITestsCI = disableITestsCI;
    }

    public Pipeline getPipeline(Map<String, String> jobEnvironmentMap) throws IntrospectionException {
        JobEnvironment jobEnvironment = JobEnvironment.create(jobEnvironmentMap);
        Pipeline pipeline = getPipeline(jobEnvironment);
        pipeline.setConfiguration(this);
        return pipeline;
    }

    public PipelineConfiguration setJobNamesCD(String... names) {
        return setJobNamesKind(PipelineKind.CD, names);
    }

    public PipelineConfiguration setJobNamesCI(String... names) {
        return setJobNamesKind(PipelineKind.CI, names);
    }

    public PipelineConfiguration setJobNamesDeveloper(String... names) {
        return setJobNamesKind(PipelineKind.Developer, names);
    }

    public PipelineConfiguration setJobNamesKind(PipelineKind kind, String... names) {
        for (String name : names) {
            jobNameToKindMap.put(name, kind);
        }
        return this;
    }

    /**
     * Adds one or more strings of the format of <code>domainName/organisationName</code> such as a String <code>github.com/fabric8io</code>
     * which is used to configure the public gitub organisation as being a environment for the given list of branch patterns
     */
    public PipelineConfiguration setCDGitOrganisation(String gitHostAndOrganisation, String... branchPatterns) {
        return setCDGitOrganisation(gitHostAndOrganisation, Arrays.asList(branchPatterns));
    }

    /**
     * Adds one or more strings of the format of <code>domainName/organisationName</code> such as a String <code>github.com/fabric8io</code>
     * which is used to configure the public gitub organisation as being a environment for the given list of branch patterns
     */
    public PipelineConfiguration setCDGitOrganisation(String gitHostAndOrganisation, List<String> branchPatterns) {
        if (branchPatterns.isEmpty()) {
            throw new IllegalArgumentException("You must specify at least one branch pattern for github host and organisation: " + gitHostAndOrganisation);
        }
        cdGitHostAndOrganisationToBranchPatterns.put(gitHostAndOrganisation, branchPatterns);
        return this;
    }

    public Pipeline getPipeline(JobEnvironment jobEnvironment) {
        String jobName = jobEnvironment.getJobName();
        PipelineKind kind = jobNameToKindMap.get(jobName);
        if (kind != null) {
            return new Pipeline(kind, jobName);
        }


        // lets figure out the defaults instead
        String branchName = jobEnvironment.getBranchName();
        kind = PipelineKind.Developer;
        if (Strings.isNullOrBlank(branchName)) {
            LOG.warn("No BranchName from the environment so cannot detect CI / PR jobs!");
        } else {
            String gitUrl = jobEnvironment.getGitUrl();
            if (Strings.isNotBlank(gitUrl)) {
                String hostOrganisation = getGitHostOrganisationString(gitUrl);
                if (Strings.isNotBlank(hostOrganisation)) {
                    List<String> branchPatterns = cdGitHostAndOrganisationToBranchPatterns.get(hostOrganisation);
                    if (branchPatterns != null && matchesPattern(branchName, branchPatterns)) {
                        return new Pipeline(PipelineKind.CD, jobName);
                    }
                }
            }

            // lets use the default branch patterns
            if (matchesPattern(branchName, ciBranchPatterns)) {
                kind = PipelineKind.CI;
            } else if (matchesPattern(branchName, cdBranchPatterns)) {
                kind = PipelineKind.CD;
            }
        }
        return new Pipeline(kind, jobName);
    }

    protected boolean matchesPattern(String text, List<String> listOfPatterns) {
        for (String pattern : listOfPatterns) {
            if (text.matches(pattern)) {
                return true;
            }
        }
        return false;
    }

    public void setSpaceNamespace(String spaceNamespace) {
        this.spaceNamespace = spaceNamespace;
    }

    public String getSpaceNamespace() {
        return spaceNamespace;
    }

    /**
     * Returns all the configuration data from the {@link ConfigMap} so that you can access additional configuration properties
     */
    public Map<String, String> getData() {
        return data;
    }

    public void setData(Map<String, String> data) {
        this.data = data;
    }
}

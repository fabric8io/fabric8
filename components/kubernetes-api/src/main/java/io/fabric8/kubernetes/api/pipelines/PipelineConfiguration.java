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

/**
 */
public class PipelineConfiguration {
    /**
     * The name of the ConfigMap which stores the {@link PipelineConfiguration}
     */
    public static final String FABRIC8_PIPELINES = "fabric8-pipelines";
    private static final transient Logger LOG = LoggerFactory.getLogger(PipelineConfiguration.class);

    private Map<String, PipelineKind> jobNameToKindMap = new HashMap<>();
    private List<String> ciBranchPatterns = new ArrayList<>();
    private List<String> cdBranchPatterns = new ArrayList<>();
    private Map<String, List<String>> cdGitHostAndOrganisationToBranchPatterns = new HashMap<>();

    public PipelineConfiguration() {
    }

    public PipelineConfiguration(Map<String, String> configMapData) {
        this.ciBranchPatterns = loadYamlListOfStrings(configMapData, "ci-branch-patterns");
        this.cdBranchPatterns = loadYamlListOfStrings(configMapData, "cd-branch-patterns");


        Map<Object, Object> orgBranchMap = loadYamlMap(configMapData, "organisation-branch-patterns");
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
                    LOG.warn("Could not find List for organisation-branch-patterns key " + key + " value: " + value);
                }
            }
        }
        Map<Object, Object> jobNameMap = loadYamlMap(configMapData, "job-name-to-kind");
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
                    LOG.warn("Ignoring job-name-to-kind key " + key + " with value: " + value
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

    public static PipelineConfiguration getPipelineConfiguration(KubernetesClient kubernetesClient, String namespace) {
        ConfigMap configMap = kubernetesClient.configMaps().inNamespace(namespace).withName(FABRIC8_PIPELINES).get();
        if (configMap != null) {
            return getPipelineConfiguration(configMap);
        }
        PipelineConfiguration configuration = createDefault();
        return configuration;
    }

    public static PipelineConfiguration getPipelineConfiguration(ConfigMap configMap) {
        PipelineConfiguration answer = new PipelineConfiguration();
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

    public Map<String, PipelineKind> getJobNameToKindMap() {
        return jobNameToKindMap;
    }

    public List<String> getCiBranchPatterns() {
        return ciBranchPatterns;
    }

    public List<String> getCdBranchPatterns() {
        return cdBranchPatterns;
    }

    public Map<String, List<String>> getCdGitHostAndOrganisationToBranchPatterns() {
        return cdGitHostAndOrganisationToBranchPatterns;
    }

    public Pipeline getPipeline(Map<String, String> jobEnvironmentMap) throws IntrospectionException {
        JobEnvironment jobEnvironment = JobEnvironment.create(jobEnvironmentMap);
        return getPipeline(jobEnvironment);
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
}

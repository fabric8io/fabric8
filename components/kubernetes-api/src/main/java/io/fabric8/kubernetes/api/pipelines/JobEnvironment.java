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

import io.fabric8.utils.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.lang.reflect.Method;
import java.util.Map;

/**
 */
public class JobEnvironment {
    private static final transient Logger LOG = LoggerFactory.getLogger(JobEnvironment.class);

    private String branchName;
    private String buildDisplayName;
    private String buildId;
    private String buildNumber;
    private String buildTag;
    private String buildUrl;
    private String jenkinsUrl;
    private String jobBaseName;
    private String jobDisplayUrl;
    private String jobName;
    private String jobUrl;
    private String gitUrl;

    public static JobEnvironment create(Map<String, String> map) throws IntrospectionException {
        JobEnvironment answer = new JobEnvironment();
        BeanInfo beanInfo = Introspector.getBeanInfo(JobEnvironment.class);
        PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
        if (propertyDescriptors != null) {
            for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                String name = propertyDescriptor.getName();
                String envVarName = propertyNameToEnvironmentVariableName(name);
                String value = map.get(envVarName);
                if (value == null) {
                    continue;
                }
                PropertyEditor propertyEditor = propertyDescriptor.createPropertyEditor(answer);
                if (propertyEditor == null) {
                    Method writeMethod = propertyDescriptor.getWriteMethod();
                    if (writeMethod == null) {
                        LOG.warn("No PropertyEditor or WriteMethod for property: " + name);
                        continue;
                    }
                    Class<?>[] parameterTypes = writeMethod.getParameterTypes();
                    if (parameterTypes.length == 0 || !parameterTypes[0].equals(String.class)) {
                        LOG.warn("WriteMethod for property: " + name + " does not take a String and there is no PropertyEditor!");
                        continue;
                    }
                    try {
                        writeMethod.invoke(answer, value);
                    } catch (Exception e) {
                        LOG.warn("Failed to set property " + name + " due to: " + e, e);
                    }
                } else {
                    propertyEditor.setAsText(value);
                }
            }
        }
        return answer;
    }

    private static String propertyNameToEnvironmentVariableName(String name) {
        return Strings.splitCamelCase(name, "_").toUpperCase();
    }


    @Override
    public String toString() {
        return "JobEnvironment{" +
                "buildId='" + buildId + '\'' +
                ", jobName='" + jobName + '\'' +
                ", gitUrl='" + gitUrl + '\'' +
                '}';
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public String getBuildDisplayName() {
        return buildDisplayName;
    }

    public void setBuildDisplayName(String buildDisplayName) {
        this.buildDisplayName = buildDisplayName;
    }

    public String getBuildId() {
        return buildId;
    }

    public void setBuildId(String buildId) {
        this.buildId = buildId;
    }

    public String getBuildNumber() {
        return buildNumber;
    }

    public void setBuildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
    }

    public String getBuildTag() {
        return buildTag;
    }

    public void setBuildTag(String buildTag) {
        this.buildTag = buildTag;
    }

    public String getBuildUrl() {
        return buildUrl;
    }

    public void setBuildUrl(String buildUrl) {
        this.buildUrl = buildUrl;
    }

    public String getJenkinsUrl() {
        return jenkinsUrl;
    }

    public void setJenkinsUrl(String jenkinsUrl) {
        this.jenkinsUrl = jenkinsUrl;
    }

    public String getJobBaseName() {
        return jobBaseName;
    }

    public void setJobBaseName(String jobBaseName) {
        this.jobBaseName = jobBaseName;
    }

    public String getJobDisplayUrl() {
        return jobDisplayUrl;
    }

    public void setJobDisplayUrl(String jobDisplayUrl) {
        this.jobDisplayUrl = jobDisplayUrl;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getJobUrl() {
        return jobUrl;
    }

    public void setJobUrl(String jobUrl) {
        this.jobUrl = jobUrl;
    }

    public String getGitUrl() {
        return gitUrl;
    }

    public void setGitUrl(String gitUrl) {
        this.gitUrl = gitUrl;
    }

}

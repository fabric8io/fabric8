/**
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.fabric8.forge.rest.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="ggastald@redhat.com">George Gastaldi</a>
 */
@XmlRootElement
public class ExecutionRequest {
    @XmlElement
    private String resource;

    @XmlElementWrapper
    private List<Map<String, String>> inputList;

    @XmlElementWrapper
    private List<String> promptQueue;

    private Integer wizardStep;

    @Override
    public String toString() {
        return "ExecutionRequest{" +
                "resource='" + resource + '\'' +
                ", inputs=" + inputList +
                ", promptQueue=" + promptQueue +
                '}';
    }

    public List<Map<String, String>> getInputList() {
        return inputList;
    }

    public void setInputList(List<Map<String, String>> inputList) {
        this.inputList = inputList;
    }

    /**
     * @return the resource
     */
    public String getResource() {
        return resource;
    }

    /**
     * @param resource the resource to set
     */
    public void setResource(String resource) {
        this.resource = resource;
    }

    public Integer getWizardStep() {
        return wizardStep;
    }

    public void setWizardStep(Integer wizardStep) {
        this.wizardStep = wizardStep;
    }

    /**
     * Returns the wizard step number or 0 if one is not defined
     */
    public int wizardStep() {
        if (wizardStep != null) {
            return wizardStep.intValue();
        } else {
            return 0;
        }
    }
}

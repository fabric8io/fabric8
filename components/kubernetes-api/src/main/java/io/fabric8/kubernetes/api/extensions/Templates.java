/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.kubernetes.api.extensions;

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.openshift.api.model.template.Parameter;
import io.fabric8.openshift.api.model.template.Template;
import io.fabric8.utils.Strings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class for working with OpenShift Templates
 */
public class Templates {
    /**
     * Allows a list of resources to be combined into a single Template if one or more templates are contained inside the list
     * or just return the unchanged list if no templates are present.
     */
    public static Object combineTemplates(KubernetesList kubernetesList) {
        Template firstTemplate = null;
        List<Object> items = kubernetesList.getItems();
        for (Object item : items) {
            if (item instanceof Template) {
                Template template = (Template) item;
                if (firstTemplate == null) {
                    firstTemplate = template;
                } else {
                    firstTemplate = combineTemplates(firstTemplate, template);
                }
            }
        }
        if (firstTemplate != null) {
            for (Object object : items) {
                if (!(object instanceof Template)) {
                    addTemplateObject(firstTemplate, object);
                }
            }
        }
        return firstTemplate != null ? firstTemplate : kubernetesList;
    }

    public static Template combineTemplates(Template firstTemplate, Template template) {
        List<Object> objects = template.getObjects();
        if (objects != null) {
            for (Object object : objects) {
                addTemplateObject(firstTemplate, object);
            }
        }
        List<Parameter> parameters = firstTemplate.getParameters();
        if (parameters == null) {
            parameters = new ArrayList<>();
            firstTemplate.setParameters(parameters);
        }
        combineParameters(parameters, template.getParameters());
        return firstTemplate;
    }

    protected static void combineParameters(List<Parameter> parameters, List<Parameter> otherParameters) {
        if (otherParameters != null && otherParameters.size() > 0) {
            Map<String, Parameter> map = new HashMap<>();
            for (Parameter parameter : parameters) {
                map.put(parameter.getName(), parameter);
            }
            for (Parameter otherParameter : otherParameters) {
                String name = otherParameter.getName();
                Parameter original = map.get(name);
                if (original == null) {
                    parameters.add(otherParameter);
                } else {
                    if (Strings.isNotBlank(original.getValue())) {
                        original.setValue(otherParameter.getValue());
                    }
                }
            }
        }
    }

    public static void addTemplateObject(Template template, Object object) {
        List<Object> objects = template.getObjects();
        if (objects == null) {
            objects = new ArrayList<>();

            template.setObjects(objects);
        }
        objects.add(object);
    }


    /**
     * If we have any templates inside the items then lets unpack them and combine any parameters
     * @param kubernetesList
     * @param items
     */
    public static Object combineTemplates(KubernetesList kubernetesList, List<Object> items) {
        Template template = null;
        for (Object item : items) {
            if (item instanceof Template) {
                Template aTemplate = (Template) item;
                if (template == null) {
                    template = aTemplate;
                } else {
                    template = combineTemplates(template, aTemplate);
                }
            }
        }
        if (template != null) {
            // lets move all the content into the template
            for (Object item : items) {
                if (!(item instanceof Template)) {
                    addTemplateObject(template, item);
                }
            }
            List<Object> objects = getTemplateObjects(template);
            KubernetesHelper.moveServicesToFrontOfArray(objects);
            return template;
        } else {
            return kubernetesList;
        }
    }

}

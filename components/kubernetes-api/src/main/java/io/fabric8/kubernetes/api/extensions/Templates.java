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

import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.openshift.api.model.template.Template;

import java.util.ArrayList;
import java.util.List;

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
                    firstTemplate.getObjects().add(object);
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
        return firstTemplate;
    }

    public static void addTemplateObject(Template template, Object object) {
        List<Object> objects = template.getObjects();
        if (objects == null) {
            objects = new ArrayList<>();
            template.setObjects(objects);
        }
        objects.add(object);
    }
}

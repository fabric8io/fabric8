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
package io.fabric8.swagger.annotator;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JEnumConstant;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JMethod;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jsonschema2pojo.Jackson2Annotator;

/**
 */
public class SwaggerAnnotator extends Jackson2Annotator {

    @Override
    public void propertyOrder(JDefinedClass clazz, JsonNode propertiesNode) {

        clazz.annotate(ToString.class);
        clazz.annotate(EqualsAndHashCode.class);
    }

    @Override
    public void propertyInclusion(JDefinedClass clazz, JsonNode schema) {

    }

    @Override
    public void propertyField(JFieldVar field, JDefinedClass clazz, String propertyName, JsonNode propertyNode) {

    }

    @Override
    public void propertyGetter(JMethod getter, String propertyName) {

    }

    @Override
    public void propertySetter(JMethod setter, String propertyName) {

    }

    @Override
    public void anyGetter(JMethod getter) {

    }

    @Override
    public void anySetter(JMethod setter) {

    }

    @Override
    public void enumCreatorMethod(JMethod creatorMethod) {

    }

    @Override
    public void enumValueMethod(JMethod valueMethod) {

    }

    @Override
    public void enumConstant(JEnumConstant constant, String value) {

    }

    @Override
    public boolean isAdditionalPropertiesSupported() {
        return true;
    }

    @Override
    public void additionalPropertiesField(JFieldVar field, JDefinedClass clazz, String propertyName) {

    }

}

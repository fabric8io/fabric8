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
package io.fabric8.tools.apt;

import org.apache.deltaspike.core.api.config.ConfigProperty;

import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Set;

/**
 * Processes all Camel {@link ConfigProperty}s and generate json schema and html documentation for the endpoint/component.
 */
@SupportedAnnotationTypes({"*"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class ConfigPropertyAnnotationProcessor extends AbstractAnnotationProcessor {

    public boolean process(Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return true;
        }
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(ConfigProperty.class);
        if (!elements.isEmpty()) {
            StringBuilder buffer = new StringBuilder("{");
            buffer.append("\n  \"type\": \"object\",");

            // TODO add schema, title, description from env...
            log("options: " + processingEnv.getOptions());

            buffer.append("\n  \"properties\": {");

            boolean first = true;
            for (Element element : elements) {
                processEndpointClass(roundEnv, element, buffer, first);
                first = false;
            }
            buffer.append("\n  }");
            buffer.append("\n}");
            buffer.append("\n");

            String text = buffer.toString();
            writeFile("io.fabric8.environment", "schema.json", text);
        }
        return true;
    }

    protected void processEndpointClass(final RoundEnvironment roundEnv, final Element element, StringBuilder buffer, boolean first) {
        final ConfigProperty property = element.getAnnotation(ConfigProperty.class);
        if (property != null) {
            String defaultValue = property.defaultValue();
            if ("org.apache.deltaspike.NullValueMarker".equals(defaultValue)) {
                defaultValue = null;
            }
            String name = property.name();

            String description = JavaDocs.getJavaDoc(getElements(), element);
            String javaTypeName = javaTypeName(element);
            String jsonType = JsonSchemaTypes.getJsonSchemaTypeName(javaTypeName);

            if (!first) {
                buffer.append(",");
            }
            buffer.append("\n    \"").append(name).append("\": {");
            buffer.append("\n      \"type\": \"").append(jsonType).append("\",");
            if (defaultValue != null) {
                buffer.append("\n      \"default\": \"").append(defaultValue).append("\",");
            }
            if (description != null) {
                description = description.trim();
                if (description.length() > 0) {
                    buffer.append("\n      \"description\": \"").append(description).append("\",");
                }
            }
            buffer.append("\n      \"javaType\": \"").append(javaTypeName).append("\"");
            buffer.append("\n    }");
        }
    }
}

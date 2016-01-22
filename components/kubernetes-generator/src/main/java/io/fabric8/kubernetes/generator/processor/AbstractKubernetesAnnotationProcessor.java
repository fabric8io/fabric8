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
package io.fabric8.kubernetes.generator.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.fabric8.kubernetes.api.model.KubernetesResource;

import javax.annotation.processing.AbstractProcessor;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class AbstractKubernetesAnnotationProcessor extends AbstractProcessor {

    private static final String KUBERNETES_JSON = "kubernetes.json";
    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    KubernetesResource readJson() {
        try {
            FileObject fileObject = processingEnv.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", KUBERNETES_JSON);
            try (Reader reader = fileObject.openReader(false)) {
                return MAPPER.readValue(reader, KubernetesResource.class);
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Kubernetes JSON not found.");
        }
        return null;
    }

    void generateJson(KubernetesResource json){
        generateJson(KUBERNETES_JSON, json);
    }
    void generateJson(String fileName, KubernetesResource json ) {
        try {
            FileObject fileObject = processingEnv.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", fileName);
            Path path = Paths.get(fileObject.toUri());
            File file = path.toFile();
            if (file.exists() && !file.delete()) {
                throw new IOException("Failed to delete old kubernetes json.");
            }
            fileObject = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", fileName);
            try (Writer writer = fileObject.openWriter()) {
                MAPPER.writeValue(writer, json);
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Error generating json.");
        }
    }

    TypeElement getClassElement(Element element) {
        if (element instanceof PackageElement) {
            throw new IllegalArgumentException("Invalid element. A package element can't be used to retrieve a class element");
        } else if (element instanceof TypeElement && element.getEnclosingElement() instanceof PackageElement) {
            return (TypeElement) element;
        } else {
            return getClassElement(element.getEnclosingElement());
        }
    }


    PackageElement getPackageElement(Element element) {
        if (element instanceof PackageElement) {
            return (PackageElement) element;
        } else {
            return getPackageElement(element.getEnclosingElement());
        }
    }
}

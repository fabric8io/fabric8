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
import io.fabric8.kubernetes.api.KubernetesHelper;
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
    private static final String KUBERNETES_YAML = "kubernetes.yml";
    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    enum FileExtension {
        JSON,
        YAML,
        UNDEFINED;

        public static FileExtension determineExtension(String extension) {
            if ("json" .equals(extension)) {
                return JSON;
            }
            else if ("yaml" .equals(extension) || "yml" .equals(extension)) {
                return YAML;
            }
            else {
                return UNDEFINED;
            }
        }
    }

    KubernetesResource readJson(String fileName) {
        try {
            FileObject fileObject = processingEnv.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", (fileName == null ? KUBERNETES_JSON : fileName));
            try (Reader reader = fileObject.openReader(false)) {
                return MAPPER.readValue(reader, KubernetesResource.class);
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, fileName + " JSON not found.");
        }
        return null;
    }

    void generateJson(KubernetesResource resource){
        generateJson(KUBERNETES_JSON, resource);
    }
    void generateJson(String fileName, KubernetesResource resource ) {
        try {
            FileObject fileObject = getFileObject(fileName);
            try (Writer writer = fileObject.openWriter()) {
                MAPPER.writeValue(writer, resource);
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Error generating json " + fileName);
        }
    }

    void generateYaml(KubernetesResource resource) {
        generateYaml(KUBERNETES_YAML, resource);
    }
    void generateYaml(String fileName, KubernetesResource resource) {
        try {
            FileObject fileObject = getFileObject(fileName);
            KubernetesHelper.saveYaml(resource, fileObject);
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Error generating json " + fileName);
        }
    }

    private FileObject getFileObject(String fileName) throws IOException {
        FileObject fileObject = processingEnv.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", fileName);
        Path path = Paths.get(fileObject.toUri());
        File file = path.toFile();
        if (file.exists() && !file.delete()) {
            throw new IOException("Failed to delete old kubernetes json file: " + fileName);
        }
        fileObject = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", fileName);
        return fileObject;
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

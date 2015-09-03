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
import io.fabric8.kubernetes.api.extensions.Templates;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.generator.annotation.KubernetesProvider;
import io.fabric8.utils.Strings;

import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.Callable;

import static io.fabric8.kubernetes.api.KubernetesHelper.getName;

@SupportedAnnotationTypes("io.fabric8.kubernetes.generator.annotation.KubernetesProvider")
public class KubernetesProviderProcessor extends AbstractKubernetesAnnotationProcessor {

    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<HasMetadata> provided = new LinkedHashSet<>();

        CompilationTaskFactory compilationTaskFactory = new CompilationTaskFactory(processingEnv);
        Set<TypeElement> providers = new HashSet<>();

        //1st pass collect classes to compile.
        for (Element element : roundEnv.getElementsAnnotatedWith(KubernetesProvider.class)) {
            providers.add(getClassElement(element));
        }

        if (providers.isEmpty()) {
            return true;
        }
        StringWriter writer = new StringWriter();
        try {
            Callable<Boolean> compileTask = compilationTaskFactory.create(providers, writer);
            if (!compileTask.call()) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to compile provider classes. See output below.");
                return false;
            }
        } catch (Exception e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Error to compile provider classes, due to: " + e.getMessage() + ". See output below.");
            return false;
        } finally {
            String output = writer.toString();
            if (Strings.isNullOrBlank(output)) {
                output = "success";
            }
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Fabric8 model generator compiler output:" + output);
        }

        //2nd pass generate json.
        for (Element element : roundEnv.getElementsAnnotatedWith(KubernetesProvider.class)) {
            try {
                if (element instanceof ExecutableElement) {
                    KubernetesProvider provider = element.getAnnotation(KubernetesProvider.class);

                    ExecutableElement methodElement = (ExecutableElement) element;
                    String methodName = methodElement.getSimpleName().toString();
                    TypeElement classElement = getClassElement(element);
                    Class<?> cls = Class.forName(classElement.getQualifiedName().toString());
                    Object instance = cls.newInstance();

                    Method providerMethod = instance.getClass().getDeclaredMethod(methodName);
                    if (providerMethod != null) {
                        Object obj = providerMethod.invoke(instance);
                        if (obj instanceof HasMetadata) {
                            provided.add((HasMetadata) obj);
                        }
                    }
                }
            } catch (Exception ex) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Error creating Kubernetes configuration.");
            }
        }

        KubernetesResource answer = null;
        try {
            answer = (KubernetesResource)KubernetesHelper.combineJson(provided);

        } catch (Exception e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to combine provider items");
            return false;
        }

        generateJson(answer);
        return true;
    }

    private KubernetesList createList(Iterable<HasMetadata> objects) {
        StringBuilder sb = new StringBuilder();
        List<HasMetadata> allItems = new ArrayList<>();
        boolean first = true;
        for (HasMetadata obj : objects) {
            if (first) {
                first = false;
            } else {
                sb.append("-");
            }
            
            sb.append(getName(obj));
            allItems.add(obj);
        }
        return new KubernetesListBuilder().
                // TODO KubernetesList no longer has an id/name
                //withName(sb.toString()).
                withItems(allItems).build();
    }
}

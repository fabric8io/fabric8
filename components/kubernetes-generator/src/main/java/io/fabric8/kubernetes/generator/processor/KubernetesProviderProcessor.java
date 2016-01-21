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

import io.fabric8.kubernetes.api.KubernetesHelper;
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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Callable;

@SupportedAnnotationTypes("io.fabric8.kubernetes.generator.annotation.KubernetesProvider")
public class KubernetesProviderProcessor extends AbstractKubernetesAnnotationProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set provided = new LinkedHashSet<>();

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
                    ExecutableElement methodElement = (ExecutableElement) element;
                    String methodName = methodElement.getSimpleName().toString();
                    TypeElement classElement = getClassElement(element);
                    Class<?> cls = Class.forName(classElement.getQualifiedName().toString());
                    Object instance = cls.newInstance();

                    Method providerMethod = instance.getClass().getDeclaredMethod(methodName);
                    if (providerMethod != null) {
                        providerMethod.setAccessible(true);
                        provided.add(providerMethod.invoke(instance));
                    }
                }
            } catch (Exception ex) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Error creating Kubernetes configuration.");
            }
        }

        KubernetesResource answer = null;
        try {
            answer = (KubernetesResource)KubernetesHelper.combineJson(provided.toArray());
        } catch (Exception e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to combine provider items");
            return false;
        }

        generateJson(answer);
        return true;
    }
}

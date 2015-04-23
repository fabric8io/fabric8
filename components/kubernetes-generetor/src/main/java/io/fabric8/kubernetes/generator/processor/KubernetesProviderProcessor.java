/*
 * Copyright 2005-2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.fabric8.kubernetes.generator.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.generator.annotation.KubernetesProvider;
import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.ImageRepository;
import io.fabric8.openshift.api.model.Route;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import javax.xml.ws.Service;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

@SupportedAnnotationTypes("io.fabric8.kubernetes.generator.annotation.KubernetesProvider")
public class KubernetesProviderProcessor extends AbstractProcessor {

    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<Object> provided = new LinkedHashSet<>();

        CompilationTaskFactory compilationTaskFactory = new CompilationTaskFactory(processingEnv);
        Set<TypeElement> providers = new HashSet<>();

        //1st pass collect classes to compile.
        for (Element element : roundEnv.getElementsAnnotatedWith(KubernetesProvider.class)) {
            providers.add(getClassElement(element));
        }

        if (providers.isEmpty()) {
            return true;
        }

        try {
            Callable<Boolean> compileTask = compilationTaskFactory.create(providers);
            if (!compileTask.call()) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to compile provider classes");
                return false;
            }
        } catch (Exception e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to compile provider classes");
            return false;
        }

        //2nd pass generate json.
        for (Element element : roundEnv.getElementsAnnotatedWith(KubernetesProvider.class)) {
            try {
                if (element instanceof ExecutableElement) {
                    KubernetesProvider provider = element.getAnnotation(KubernetesProvider.class);

                    ExecutableElement methodElement = (ExecutableElement) element;
                    TypeElement classElement = getClassElement(element);
                    Class<?> cls = Class.forName(classElement.getQualifiedName().toString());
                    Object instance = cls.newInstance();
                    Method providerMethod = instance.getClass().getDeclaredMethod(methodElement.getSimpleName().toString());
                    if (providerMethod != null) {
                        Object obj = providerMethod.invoke(instance);
                        if (obj != null) {
                            provided.add(obj);
                        }
                    }
                }
            } catch (Exception ex) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Error creating Kubernetes configuration.");
            }
        }

        KubernetesList list = createList(provided);
        generateJson(list);
        return true;
    }


    private void generateJson(KubernetesList list) {
        try {
            FileObject fileObject = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", "kubernetes.json");
            try (Writer writer = fileObject.openWriter()) {
                MAPPER.writeValue(writer, list);
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Error generating json.");
        }
    }

    private KubernetesList createList(Iterable<Object> objects) {
        StringBuilder sb = new StringBuilder();
        List<Object> allItems = new ArrayList<>();
        boolean first = true;
        for (Object obj : objects) {
            
            if (first) {
                first = false;
            } else {
                sb.append("-");
            }
            
            if (obj instanceof Pod) {
                sb.append(((Pod) obj).getId());
                allItems.add(obj);
            } else if (obj instanceof ReplicationController) {
                sb.append(((ReplicationController) obj).getId());
                allItems.add(obj);
            }  else if (obj instanceof Service) {
                sb.append(((Service) obj).getServiceName());
                allItems.add(obj);
            } else if (obj instanceof BuildConfig) {
                sb.append(((BuildConfig) obj).getName());
                allItems.add(obj);
            } else if (obj instanceof DeploymentConfig) {
                sb.append(((DeploymentConfig) obj).getName());
                allItems.add(obj);
            } else if (obj instanceof ImageRepository) {
                sb.append(((ImageRepository) obj).getName());
                allItems.add(obj);
            } else if (obj instanceof Route) {
                sb.append(((Route) obj).getName());
                allItems.add(obj);
            } else if (obj instanceof KubernetesList) {
                sb.append(((KubernetesList) obj).getId());
                allItems.addAll(((KubernetesList) obj).getItems());
            }
        }
        return new KubernetesListBuilder().withId(sb.toString()).withItems(allItems).build();
    }

    public static TypeElement getClassElement(Element element) {
        if (element instanceof PackageElement) {
            throw new IllegalArgumentException("Invalid element. A package element can't be used to retrieve a class element");
        } else if (element instanceof TypeElement && element.getEnclosingElement() instanceof PackageElement) {
            return (TypeElement) element;
        } else {
            return getClassElement(element.getEnclosingElement());
        }
    }


    public static PackageElement getPackageElement(Element element) {
        if (element instanceof PackageElement) {
            return (PackageElement) element;
        } else {
            return getPackageElement(element.getEnclosingElement());
        }
    }
}

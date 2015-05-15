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

import io.fabric8.common.Visitor;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.generator.annotation.KubernetesModelProcessor;

import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Callable;

@SupportedAnnotationTypes("io.fabric8.kubernetes.generator.annotation.KubernetesModelProcessor")
public class KubernetesModelProcessorProcessor extends AbstractKubernetesAnnotationProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<Object> provided = new LinkedHashSet<>();

        CompilationTaskFactory compilationTaskFactory = new CompilationTaskFactory(processingEnv);
        Set<TypeElement> processors = new HashSet<>();

        //1st pass collect classes to compile.
        for (Element element : roundEnv.getElementsAnnotatedWith(KubernetesModelProcessor.class)) {
            processors.add(getClassElement(element));
        }

        if (processors.isEmpty()) {
            return true;
        }

        try {
            Callable<Boolean> compileTask = compilationTaskFactory.create(processors);
            if (!compileTask.call()) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to compile provider classes");
                return false;
            }
        } catch (Exception e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to compile provider classes");
            return false;
        }

        //2nd pass generate json.
        for (Element element : roundEnv.getElementsAnnotatedWith(KubernetesModelProcessor.class)) {
            KubernetesListBuilder builder = new KubernetesListBuilder(readJson());
            try {
                if (element instanceof TypeElement) {
                    KubernetesModelProcessor processor = element.getAnnotation(KubernetesModelProcessor.class);

                    for (ExecutableElement methodElement :ElementFilter.methodsIn(element.getEnclosedElements())) {

                        TypeElement classElement = getClassElement(element);
                        Class<?> cls = Class.forName(classElement.getQualifiedName().toString());
                        final Object instance = cls.newInstance();
                        final String methodName = methodElement.getSimpleName().toString();

                        builder.accept(new Visitor() {
                            @Override
                            public void visit(Object o) {
                              for (Method m :findMethods(instance, methodName, o.getClass())) {
                                  try {
                                      m.invoke(instance, o);
                                  } catch (IllegalAccessException e) {
                                      processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Error invoking visitor method:" + m.getName() + " on:" + instance + "with argument:" + o);
                                  } catch (InvocationTargetException e) {
                                      processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Error invoking visitor method:" + m.getName() + " on:" + instance + "with argument:" + o);
                                  }
                              }
                            }
                        });
                    }
                }
                KubernetesList list = builder.build();
                generateJson(list);
            } catch (Exception ex) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Error creating Kubernetes configuration:" + ex.getMessage());
            }
        }

        return true;
    }


    private Set<Method> findMethods(Object instance, String methodName, Class argumentType) {
        Set<Method> result = new LinkedHashSet<>();

        for (Method m :instance.getClass().getDeclaredMethods()) {
            if (m.getName().equals(methodName) && m.getParameterTypes().length == 1 && m.getParameterTypes()[0].isAssignableFrom(argumentType)) {
                result.add(m);
            }
        }
        return result;
    }

}

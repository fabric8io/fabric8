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
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.generator.annotation.KubernetesModelProcessor;
import io.fabric8.utils.Maps;
import io.fabric8.utils.Strings;

import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.inject.Named;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
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
                    final KubernetesModelProcessor processor = element.getAnnotation(KubernetesModelProcessor.class);

                    for (ExecutableElement methodElement :ElementFilter.methodsIn(element.getEnclosedElements())) {

                        TypeElement classElement = getClassElement(element);
                        Class<?> cls = Class.forName(classElement.getQualifiedName().toString());
                        final Object instance = cls.newInstance();
                        final String methodName = methodElement.getSimpleName().toString();

                        builder.accept(new Visitor() {
                            @Override
                            public void visit(Object o) {
                              for (Method m :findMethods(instance, methodName, o.getClass())) {
                                  Named named = m.getAnnotation(Named.class);
                                  if (named != null && !Strings.isNullOrBlank(named.value())) {
                                      String objectName = getName(o);
                                      //If a name has been explicitly specified check if there is a match
                                      if (!named.value().equals(objectName)) {
                                          processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                                                  "Named method:" + m.getName() + " with name:" + named.value() + " doesn't match: " + objectName + ", ignoring");
                                          return;
                                      }
                                  }
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


    private static Set<Method> findMethods(Object instance, String methodName, Class argumentType) {
        Set<Method> result = new LinkedHashSet<>();

        for (Method m :instance.getClass().getDeclaredMethods()) {
            if (m.getName().equals(methodName) && m.getParameterTypes().length == 1 && m.getParameterTypes()[0].isAssignableFrom(argumentType)) {
                result.add(m);
            }
        }
        return result;
    }

    private static <T, V> V getWithReflection(T object, Class<V> clazz, String methodNamed) {
        if (object == null) {
            return null;
        } else {
            try {
                Method method = object.getClass().getMethod(methodNamed);
                return clazz.cast(method.invoke(object));
            } catch (Exception e) {
                return null;
            }
        }
    }
    public static <T> String getUuid(T obj) {
        return getWithReflection(obj,  String.class, "getUid");
    }

    public static <T> Map<String, Object> getAdditionalProperties(T obj) {
        return getWithReflection(obj,  Map.class, "getAdditionalProperties");
    }

    public static <T> ObjectMeta getObjectMeta(T obj) {
        return getWithReflection(obj, ObjectMeta.class, "getMetadata");
    }

    public static <T>  io.fabric8.kubernetes.api.model.base.ObjectMeta getBaseMeta(T obj) {
        return getWithReflection(obj, io.fabric8.kubernetes.api.model.base.ObjectMeta.class, "getMetadata");
    }

    public static <T> String getName(T entity) {
        if (entity != null) {
            Map<String, Object> additionalProperties = getAdditionalProperties(entity);
            return Strings.firstNonBlank(
                    getWithReflection(entity, String.class, "getName"),
                    getName(getObjectMeta(entity)),
                    getName(getBaseMeta(entity)),
                    Maps.nestedValueAsString(additionalProperties, "metadata", "id"),
                    Maps.nestedValueAsString(additionalProperties, "metadata", "name"),
                    String.valueOf(additionalProperties.get("id")),
                    getUuid(entity));
        } else {
            return null;
        }
    }
}

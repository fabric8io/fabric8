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
package io.fabric8.kubernetes.generator.processor;


import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class CompilationTaskFactory {

    private static final String FILE_URL_PREFIX = "file:";
    private static final String DISABLE_ANNOTATION_PROCESSORS_OPT = "-proc:none";
    private static final String CLASSPATH_OPT = "-classpath";
    private static final String FILE_MANAGER_FIELD_NAME = "fileManager";

    private final DiagnosticListener<JavaFileObject> diagnosticListener = new DiagnosticCollector<>();
    private final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

    private final ProcessingEnvironment processingEnvironment;
    private final JavaFileManager fileManager;

    public CompilationTaskFactory(ProcessingEnvironment processingEnvironment) {
        this.processingEnvironment = processingEnvironment;
        this.fileManager = createFileManager(processingEnvironment);
    }

    /**
     * Obtains the {@link javax.tools.JavaFileManager} from the {@link javax.annotation.processing.ProcessingEnvironment}/
     * @param processingEnvironment The processing environment.
     * @return                      The file manager.
     */
    static JavaFileManager createFileManager(ProcessingEnvironment processingEnvironment) {
        Filer filer = processingEnvironment.getFiler();
        try {
            Field field = filer.getClass().getDeclaredField(FILE_MANAGER_FIELD_NAME);
            field.setAccessible(true);
            return (JavaFileManager) field.get(filer);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Creates a compilation task for the specified {@link javax.lang.model.element.Element} instances.
     * @param elements          The elements.
     * @return                  The compilation task.
     * @throws java.io.IOException
     */
    public JavaCompiler.CompilationTask create(Iterable<TypeElement> elements, Writer writer) throws IOException {
        Set<String> options = new LinkedHashSet<>();
        Set<JavaFileObject> javaFileObjects = new LinkedHashSet<>();

        ClassLoader classLoader = getClass().getClassLoader();
        if (classLoader instanceof URLClassLoader) {
            String classPath = createClassPath(((URLClassLoader) classLoader).getURLs());
            options.add(DISABLE_ANNOTATION_PROCESSORS_OPT);
            options.add(CLASSPATH_OPT);
            options.add(classPath);
        }

        for (TypeElement element : elements) {
            JavaFileObject source = fileManager.getJavaFileForInput(StandardLocation.SOURCE_PATH, element.getQualifiedName().toString(), JavaFileObject.Kind.SOURCE);
            if (source == null) {
                throw new IOException("Unable to find class: " + element.getQualifiedName().toString());
            }

            javaFileObjects.add(source);
        }
        return compiler.getTask(writer, fileManager, diagnosticListener, options, new ArrayList<String>(), javaFileObjects);
    }
    
    private static String createClassPath(URL[] urls) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (URL url : urls) {
            if (first) {
                first=false;
            } else {
                sb.append(File.pathSeparator);
            }
            sb.append(url.toExternalForm().replaceFirst(FILE_URL_PREFIX, ""));
        }
        return sb.toString();
    }

    public List<Diagnostic> getCompileDiagnostics() {
        return ((DiagnosticCollector)diagnosticListener).getDiagnostics();
    }
}

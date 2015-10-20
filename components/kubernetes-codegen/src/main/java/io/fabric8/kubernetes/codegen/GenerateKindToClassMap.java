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
package io.fabric8.kubernetes.codegen;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Generates the Java source for mapping kinds to classes from the generated Kubernetes schema
 */
public class GenerateKindToClassMap {
    public static void main(String[] args) throws Exception {
        ClassPath classPath = ClassPath.from(GenerateKindToClassMap.class.getClassLoader());
        SortedMap<String, String> sortedMap = new TreeMap<>();
        String[] topLevelPackages = {"io.fabric8.kubernetes.api.model", "io.fabric8.openshift.api.model"};
        for (String topLevelPackage : topLevelPackages) {
            ImmutableSet<ClassPath.ClassInfo> classInfos = classPath.getTopLevelClassesRecursive(topLevelPackage);
            for (ClassPath.ClassInfo classInfo : classInfos) {
                String simpleName = classInfo.getSimpleName();
                if (simpleName.endsWith("Builder") || simpleName.endsWith("Fluent")) {
                    continue;
                }
                sortedMap.put(simpleName, classInfo.getName());
            }
        }
        String basedir = System.getProperty("basedir", ".");
        File file = new File(basedir, "../kubernetes-api/src/main/java/io/fabric8/kubernetes/api/support/KindToClassMapping.java");
        file.getParentFile().mkdirs();
        System.out.println("Generating " + file);

        SortedSet<String> classNames = new TreeSet<>(sortedMap.values());
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            Set<Map.Entry<String, String>> entries = sortedMap.entrySet();
            writer.println("/**\n" +
                    " *  Copyright 2005-2015 Red Hat, Inc.\n" +
                    " *\n" +
                    " *  Red Hat licenses this file to you under the Apache License, version\n" +
                    " *  2.0 (the \"License\"); you may not use this file except in compliance\n" +
                    " *  with the License.  You may obtain a copy of the License at\n" +
                    " *\n" +
                    " *     http://www.apache.org/licenses/LICENSE-2.0\n" +
                    " *\n" +
                    " *  Unless required by applicable law or agreed to in writing, software\n" +
                    " *  distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
                    " *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or\n" +
                    " *  implied.  See the License for the specific language governing\n" +
                    " *  permissions and limitations under the License.\n" +
                    " */\n" +
                    "package io.fabric8.kubernetes.api.support;\n" +
                    "\n");
            for (String className : classNames) {
                writer.println("import " + className + ";");
            }

            writer.println("\n" +
                    "import java.util.HashMap;\n" +
                    "import java.util.Map;\n" +
                    "\n" +
                    "/**\n" +
                    " * Maps the Kubernetes kinds to the Jackson DTO classes\n" +
                    " */\n" +
                    "public class KindToClassMapping {\n" +
                    "    private static Map<String,Class<?>> map = new HashMap<>();\n" +
                    "\n" +
                    "    static {");


            for (Map.Entry<String, String> entry : entries) {
                String kind = entry.getKey();
                String className = entry.getValue();
                writer.println("        map.put(\"" + kind + "\", " + kind + ".class);");
            }

            writer.println("    }\n" +
                    "\n" +
                    "    public static Map<String,Class<?>> getKindToClassMap() {\n" +
                    "        return map;\n" +
                    "    }\n" +
                    "}\n");
        }
    }
}

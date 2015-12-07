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
package io.fabric8.forge.addon.utils;

import java.net.URLClassLoader;

import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.facets.ClassLoaderFacet;

public final class JavaHelper {

    /**
     * Removes the package from the type name from the given type
     */
    public static String removeJavaPackageName(String className) {
        int idx = className.lastIndexOf('.');
        if (idx >= 0) {
            return className.substring(idx + 1);
        } else {
            return className;
        }
    }

    public static URLClassLoader getProjectClassLoader(Project project) {
        if (project != null) {
            ClassLoaderFacet classLoaderFacet = project.getFacet(ClassLoaderFacet.class);
            if (classLoaderFacet != null) {
                return classLoaderFacet.getClassLoader();
            }
        }
        return null;
    }

    public static boolean projectHasClassOnClassPath(Project project, String className) {
        URLClassLoader classLoader = getProjectClassLoader(project);
        if (classLoader != null ){
            try {
                Class<?> clazz = classLoader.loadClass(className);
                return true;
            } catch (ClassNotFoundException e) {
                // ignore
            }
        }
        return false;
    }

    /**
     * Loads a class of the given name from the project class loader or returns null if its not found
     */
    public static Class<?> loadProjectClass(Project project, String className) {
        URLClassLoader classLoader = getProjectClassLoader(project);
        if (classLoader != null ){
            try {
                return classLoader.loadClass(className);
            } catch (ClassNotFoundException e) {
                // ignore
            }
        }
        return null;
    }
}

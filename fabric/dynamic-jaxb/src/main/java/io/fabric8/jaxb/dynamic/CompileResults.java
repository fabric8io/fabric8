/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.jaxb.dynamic;

import javax.xml.bind.JAXBContext;

import org.apache.cxf.common.classloader.ClassLoaderUtils;

/**
 * Returns the result of the compile of the XML Schemas
 */
public class CompileResults {

    private final ClassLoader classLoader;
    private final JAXBContext context;

    public CompileResults(ClassLoader classLoader, JAXBContext context) {
        this.classLoader = classLoader;
        this.context = context;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public JAXBContext getJAXBContext() {
        return context;
    }

    /**
     * Sets the thread context class loader to the current class loader for the newly created
     * JAXB beans
     */
    public void setContextClassLoader() {
        ClassLoaderUtils.setThreadContextClassloader(classLoader);
    }
}

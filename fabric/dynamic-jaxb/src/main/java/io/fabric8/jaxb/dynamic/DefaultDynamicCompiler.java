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

import java.util.Collection;

import org.apache.camel.util.ObjectHelper;

import static org.apache.camel.util.ObjectHelper.notNull;

/**
 * Performs the dynamic compile once on startup
 */
public class DefaultDynamicCompiler implements DynamicCompiler {
    private String[] urls;
    private ClassLoader classLoader;

    public static CompileResults doCompile(ClassLoader classLoader, Collection<String> urls) {
        String[] array = urls.toArray(new String[urls.size()]);
        DefaultDynamicCompiler compiler = new DefaultDynamicCompiler(classLoader, array);
        return compiler.compile();
    }

    public DefaultDynamicCompiler() {
    }

    public DefaultDynamicCompiler(String... urls) {
        this.urls = urls;
    }

    public DefaultDynamicCompiler(ClassLoader classLoader, String[] urls) {
        this.classLoader = classLoader;
        this.urls = urls;
    }

    public void setHandler(CompileResultsHandler handler) {
        ObjectHelper.notNull(handler, "handler");
        CompileResults compileResults = compile();
        handler.onCompileResults(compileResults);
    }

    public CompileResults compile() {
        notNull(urls, "urls");

        DynamicXJC xjc = new DynamicXJC(getClassLoader());
        for (String url : urls) {
            xjc.addSchemaUrl(url);
        }
        CompileResults compileResults = xjc.compileSchemas();
        return compileResults;
    }


    // Properties
    //-------------------------------------------------------------------------
    public ClassLoader getClassLoader() {
        if (classLoader == null) {
            return getClass().getClassLoader();
        }
        return classLoader;
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public String[] getUrls() {
        return urls;
    }

    public void setUrls(String[] urls) {
        this.urls = urls;
    }

}

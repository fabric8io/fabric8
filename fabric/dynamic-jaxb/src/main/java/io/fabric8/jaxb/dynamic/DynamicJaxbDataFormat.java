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

import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.apache.camel.util.ObjectHelper;

/**
 * An implementation of {@link JaxbDataFormat} which can deal with compiling XSD files on the fly
 * to create the JAXB context
 */
public class DynamicJaxbDataFormat extends JaxbDataFormat implements CompileResultsHandler {
    private DynamicCompiler compiler;
    private CompileResults compileResults;

    public DynamicJaxbDataFormat() {
    }

    public DynamicJaxbDataFormat(JAXBContext context) {
        super(context);
    }

    public DynamicJaxbDataFormat(String contextPath) {
        super(contextPath);
    }

    public DynamicCompiler getCompiler() {
        return compiler;
    }

    public void setCompiler(DynamicCompiler compiler) throws Exception {
        this.compiler = compiler;
        if (compiler != null) {
            compiler.setHandler(this);
        }
    }

    public void init() throws Exception {
        ObjectHelper.notNull(getCompiler(), "compiler");
    }

    /**
     * Whenever any kind of compiler is performed this is the handler method to pass in the results
     */
    @Override
    public void onCompileResults(CompileResults compileResults) {
        this.compileResults = compileResults;
        setContext(compileResults.getJAXBContext());
    }

    /**
     * Returns the class loader created by the dynamic compilation or null if none has been created yet
     */
    public ClassLoader getClassLoader() {
        if (compileResults != null) {
            return compileResults.getClassLoader();
        }
        return null;
    }

    public CompileResults getCompileResults() {
        return compileResults;
    }
}

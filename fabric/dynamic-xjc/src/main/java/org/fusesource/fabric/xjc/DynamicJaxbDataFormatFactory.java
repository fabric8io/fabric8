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
package org.fusesource.fabric.xjc;

import static org.apache.camel.util.ObjectHelper.notNull;

/**
 * A factory for creating a new {@link DynamicJaxbDataFormat} by compiling the given XSDs on the fly with XJC
 * to new bytecode and loading it on the fly
 */
public class DynamicJaxbDataFormatFactory {
    private String[] urls;
    private ClassLoader classLoader;

    public DynamicJaxbDataFormatFactory() {
    }

    public DynamicJaxbDataFormatFactory(String... urls) {
        this.urls = urls;
    }

    public DynamicJaxbDataFormat createDataFormat() {
        notNull(urls, "urls");

        DynamicJaxbDataFormat dataFormat = new DynamicJaxbDataFormat();

        DynamicXJC xjc = new DynamicXJC(getClassLoader());
        for (String url : urls) {
            xjc.addSchemaUrl(url);
        }
        xjc.compileAndUpdate(dataFormat);

        return dataFormat;
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

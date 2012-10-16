/*
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
package org.apache.directory.server.xbean.blueprint;

import java.beans.PropertyEditor;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.mutable.MutableBeanMetadata;
import org.apache.directory.server.configuration.ApacheDS;
import org.apache.directory.server.xdbm.Index;
import org.apache.xbean.blueprint.context.impl.XBeanNamespaceHandler;
import org.osgi.framework.Bundle;
import org.osgi.service.blueprint.container.Converter;
import org.osgi.service.blueprint.container.ReifiedType;
import org.osgi.service.blueprint.reflect.Metadata;
import org.w3c.dom.Element;

public class DirectoryNamespaceHandler extends XBeanNamespaceHandler {

    public DirectoryNamespaceHandler(String namespace, URL schemaLocation, Set<Class> managedClasses, Map<String, Class<? extends PropertyEditor>> propertyEditors, Properties properties) {
        super(namespace, schemaLocation, managedClasses, propertyEditors, properties);
    }

    public DirectoryNamespaceHandler(String namespace, String schemaLocation, Bundle bundle, String propertiesLocation) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        super(namespace, schemaLocation, bundle, propertiesLocation);
    }

    public DirectoryNamespaceHandler(String namespace, String schemaLocation, String propertiesLocation) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        super(namespace, schemaLocation, propertiesLocation);
    }

    @Override
    public Metadata parse(Element element, ParserContext parserContext) {
        if (parserContext.getComponentDefinitionRegistry().getComponentDefinition("." + MyConverter.class.getName()) == null) {
            MutableBeanMetadata converter = parserContext.createMetadata(MutableBeanMetadata.class);
            converter.setId("." + MyConverter.class.getName());
            converter.setRuntimeClass(MyConverter.class);
            parserContext.getComponentDefinitionRegistry().registerTypeConverter(converter);
        }
        Metadata metadata = super.parse(element, parserContext);
        if (metadata instanceof MutableBeanMetadata) {
            MutableBeanMetadata bean = (MutableBeanMetadata) metadata;
            if (bean.getRuntimeClass() == ApacheDS.class) {
                bean.setInitMethod("startup");
                bean.setDestroyMethod("shutdown");
            }
        }
        return metadata;
    }

    public static class MyConverter implements Converter {

        public boolean canConvert(Object sourceObject, ReifiedType targetType) {
            if (targetType.getRawClass() == Index.class) {
                return true;
            }
            return false;
        }

        public Object convert(Object sourceObject, ReifiedType targetType) throws Exception {
            if (targetType.getRawClass().isInstance(sourceObject)) {
                return sourceObject;
            }
            return sourceObject;
        }

    }

}

/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.api.scr;

import io.fabric8.api.scr.support.ConfigInjection;
import io.fabric8.api.scr.support.ConverterHelper;
import io.fabric8.api.scr.support.ReflectionHelper;
import io.fabric8.api.scr.support.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/*
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
*/

/**
 * An abstract base class used to perform field injection of configuration admin properties
 * on a Component when it is activated or the configuration is modified.
 * <p>
 * This component discovers the OSGi MetaType XML or OSGi Declarative Services XML files and uses
 * those XML files to determine which fields require injection from with property values in the Config Admin PID of the @Component.
 * <p>
 * As a component developer, you typically need to annotate the fields in your @Component object with
 * @Property from the
 * <a href="http://felix.apache.org/documentation/subprojects/apache-felix-maven-scr-plugin/scr-annotations.html">Felix SCR Annotations</a>
 * or create the OSGi MetaType or Declarative Services XML files by hand.
 */
public abstract class AbstractFieldInjectionComponent extends AbstractComponent {
    private static final transient Logger LOG = LoggerFactory.getLogger(AbstractFieldInjectionComponent.class);

    //@Activate
    public void activate(Map<String, ?> configuration) throws Exception {
        ConfigInjection.applyConfiguration(configuration, this);
        activateComponent();
        onConfigured();
        onActivate();
    }

    //@Modified
    public void modified(Map<String, ?> configuration) throws Exception {
        ConfigInjection.applyConfiguration(configuration, this);
        onModified();
    }

    //@Deactivate
    public void deactivate() throws Exception {
        deactivateComponent();
        onDeactivate();
    }



    /**
     * Injects the given AD element configuration value into the instance, trying to find a field or setter method for it
     */
    protected void injectMetaTypePropertyValue(Class<?> clazz, Object instance, String name, String defaultValue, Map<String, ?> configuration) {
        if (Strings.isNullOrBlank(name)) {
            return;
        }
        Object value = configuration.get(name);
        if (value == null) {
            value = defaultValue;
        }
        try {
            Field field = clazz.getDeclaredField(name);
            if (field != null) {
                Object convertedValue = ConverterHelper.convertValue(value, field.getType());
                if (convertedValue != null) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Injecting value " + convertedValue + " to field " + field);
                    }
                    ReflectionHelper.setField(field, instance, convertedValue);
                }
            }
        } catch (NoSuchFieldException e) {
            // ignore
        }
    }

    /**
     * Strategy pattern for after this component has been activated
     */
    protected void onActivate() throws Exception {
    }

    /**
     * Strategy pattern for after this component has been modified
     */
    protected void onModified() throws Exception {
    }

    /**
     * Strategy pattern for after this component has been deactivated
     */
    protected void onDeactivate() throws Exception {
    }

    /**
     * Strategy pattern for after this component has been configured (either on @Activate or @Modified)
     */
    protected void onConfigured() throws Exception {
    }

}

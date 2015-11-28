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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.jboss.forge.addon.convert.Converter;
import org.jboss.forge.addon.convert.ConverterFactory;
import org.jboss.forge.addon.ui.facets.HintsFacet;
import org.jboss.forge.addon.ui.input.InputComponent;
import org.jboss.forge.addon.ui.input.InputComponentFactory;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.input.UISelectOne;

public final class UIHelper {

    /**
     * Create the input widget to use for the given option.
     *
     * @return the input widget, or <tt>null</tt> if not supported because of inputClazz not possible to be used
     */
    @SuppressWarnings("unchecked")
    public static InputComponent createUIInput(InputComponentFactory factory, ConverterFactory converterFactory, String name, Class inputClazz,
                                               String required, String currentValue, String defaultValue, String enums, String description) {

        // is the current value a property placeholder, then we need to use a regular text based UI
        if (currentValue != null && currentValue.startsWith("{{") && currentValue.endsWith("}}")) {
            // switch over to plain string
            inputClazz = String.class;
            enums = null;
        }

        InputComponent input;
        if (enums != null) {
            UISelectOne ui = factory.createSelectOne(name, inputClazz);
            // the enums are comma separated
            List<String> enumValues = new ArrayList<>();
            String[] values = enums.split(",");
            for (String v : values) {
                enumValues.add(v);
            }
            ui.setValueChoices(enumValues);
            if (defaultValue != null) {
                Object value = defaultValue;
                Converter converter = converterFactory.getConverter(String.class, inputClazz);
                if (converter != null) {
                    value = converter.convert(defaultValue);
                }
                ui.setDefaultValue(value);
            }
            if (currentValue != null) {
                Object value = currentValue;
                Converter converter = converterFactory.getConverter(String.class, inputClazz);
                if (converter != null) {
                    value = converter.convert(currentValue);
                }
                // set the value from the enum choices so the UI can indicate this correctly in the UI
                for (String v : enumValues) {
                    if (v.equalsIgnoreCase((String) value)) {
                        value = v;
                        break;
                    }
                }
                ui.setValue(value);
            }
            // This will always prompt, regardless if there is a value set
            Iterator it = ui.getFacets().iterator();
            while (it.hasNext()) {
                Object facet = it.next();
                if (facet instanceof HintsFacet) {
                    ((HintsFacet) facet).setPromptInInteractiveMode(true);
                }
            }

            input = ui;
        } else {
            UIInput ui = factory.createInput(name, inputClazz);
            if (defaultValue != null) {
                Object value = defaultValue;
                Converter converter = converterFactory.getConverter(String.class, inputClazz);
                if (converter != null) {
                    value = converter.convert(defaultValue);
                }
                ui.setDefaultValue(value);
            }
            if (currentValue != null) {
                Object value = currentValue;
                Converter converter = converterFactory.getConverter(String.class, inputClazz);
                if (converter != null) {
                    value = converter.convert(currentValue);
                }
                ui.setValue(value);
            }

            // This will always prompt, regardless if there is a value set
            Iterator it = ui.getFacets().iterator();
            while (it.hasNext()) {
                Object facet = it.next();
                if (facet instanceof HintsFacet) {
                    ((HintsFacet) facet).setPromptInInteractiveMode(true);
                }
            }

            input = ui;
        }

        if (Objects.equals("true", required)) {
            input.setRequired(true);
        }
        String label = asTitleCase(name);
        input.setLabel(label);
        // must use an empty description otherwise the UI prints null
        input.setDescription(description != null ? description : "");

        return input;
    }

    /**
     * Returns the text in human readable title/camel case format
     */
    public static String asTitleCase(String text) {
        // see: http://stackoverflow.com/questions/2559759/how-do-i-convert-camelcase-into-human-readable-names-in-java
        text = text.replaceAll(
                String.format("%s|%s|%s",
                        "(?<=[A-Z])(?=[A-Z][a-z])",
                        "(?<=[^A-Z])(?=[A-Z])",
                        "(?<=[A-Za-z])(?=[^A-Za-z])"
                ),
                " "
        );

        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

}

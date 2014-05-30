/**
 *  Copyright 2005-2014 Red Hat, Inc.
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
package io.fabric8.process.spring.boot.starter.camel;

import org.apache.camel.component.properties.PropertiesComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.PropertyResolver;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class SpringPropertiesComponent extends PropertiesComponent {

    private static final Pattern CAMEL_PLACEHOLDERS_PATTERN = Pattern.compile("\\{\\{(.+?)\\}\\}");

    @Autowired
    private PropertyResolver propertyResolver;

    @Override
    public String parseUri(String uri) throws Exception {
        Matcher placeholdersMatcher = CAMEL_PLACEHOLDERS_PATTERN.matcher(uri);
        while(placeholdersMatcher.find()) {
            String placeholder = placeholdersMatcher.group(1);
            String resolvedPlaceholder = propertyResolver.getProperty(placeholder);
            uri = uri.replace("{{" + placeholder + "}}", resolvedPlaceholder);
        }
        return uri;
    }

}

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
package io.fabric8.docker.api.support;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.AnnotatedParameter;

/**
 */
public class DockerPropertyNamingStrategy extends PropertyNamingStrategy {
    @Override
    public String nameForGetterMethod(MapperConfig<?> mapperConfig, AnnotatedMethod annotatedMethod, String name) {
        return capitalise(super.nameForGetterMethod(mapperConfig, annotatedMethod, name));

    }

    @Override
    public String nameForField(MapperConfig<?> mapperConfig, AnnotatedField annotatedField, String name) {
        return capitalise(super.nameForField(mapperConfig, annotatedField, name));
    }

    @Override
    public String nameForSetterMethod(MapperConfig<?> mapperConfig, AnnotatedMethod annotatedMethod, String name) {
        return capitalise(super.nameForSetterMethod(mapperConfig, annotatedMethod, name));
    }

    @Override
    public String nameForConstructorParameter(MapperConfig<?> mapperConfig, AnnotatedParameter annotatedParameter, String name) {
        return capitalise(super.nameForConstructorParameter(mapperConfig, annotatedParameter, name));
    }

    protected static String capitalise(String text) {
        if (text != null && text.length() > 0) {
            return text.substring(0, 1).toUpperCase() + text.substring(1);
        }
        return text;
    }
}

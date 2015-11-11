/*
 * Copyright 2005-2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.fabric8.cdi;

import io.fabric8.annotations.PortName;
import io.fabric8.annotations.Protocol;
import io.fabric8.annotations.ServiceName;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.utils.Strings;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class Utils {

    public static String or(String ... candidates) {
        for (String candidate : candidates) {
            if (Strings.isNotBlank(candidate)) {
                return candidate;
            }
        }
        return null;
    }


    static String getFactoryMethodProtocol(Method method) {
        for (Annotation[] annotations : method.getParameterAnnotations()) {
            Boolean hasServiceName = false;
            String protocol = null;
            for (Annotation annotation : annotations) {
                if (annotation.annotationType().equals(ServiceName.class)) {
                    hasServiceName = true;
                } else if (annotation.annotationType().equals(Protocol.class)) {
                    protocol = readAnnotationValue(annotation.toString());
                }

                if (hasServiceName && protocol != null) {
                    return protocol;
                }
            }
        }
        return null;
    }

    static String getFactoryMethodPort(Method method) {
        for (Annotation[] annotations : method.getParameterAnnotations()) {
            Boolean hasServiceName = false;
            String port = null;
            for (Annotation annotation : annotations) {
                if (annotation.annotationType().equals(ServiceName.class)) {
                    hasServiceName = true;
                } else if (annotation.annotationType().equals(PortName.class)) {
                    port = readAnnotationValue(annotation.toString());
                }

                if (hasServiceName && port != null) {
                    return port;
                }
            }
        }
        return null;
    }

     static String readAnnotationValue(String annotation) {
        String result = annotation;
        try {
            result = result.substring(result.indexOf("value=") + 6);
            result = result.substring(0, result.lastIndexOf(")"));
        } catch (Exception e) {
            return null;
        }
        return result;
    }
}

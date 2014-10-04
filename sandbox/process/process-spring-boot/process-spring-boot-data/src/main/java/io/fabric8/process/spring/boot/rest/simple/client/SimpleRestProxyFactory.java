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
package io.fabric8.process.spring.boot.rest.simple.client;

import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import static java.lang.Character.toLowerCase;
import static java.lang.reflect.Proxy.newProxyInstance;

public class SimpleRestProxyFactory {

    private final RestOperations restOperations;

    public SimpleRestProxyFactory(RestOperations restOperations) {
        this.restOperations = restOperations;
    }

    public SimpleRestProxyFactory() {
        this.restOperations = new RestTemplate();
    }

    @SuppressWarnings("unchecked")
    public <T> RestProxy<T> proxyService(final Class<T> serviceClass, final String baseServiceUrl) {
        return (RestProxy<T>) newProxyInstance(SimpleRestProxyFactory.class.getClassLoader(), new Class[]{RestProxy.class}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String normalizedBaseServiceUrl = normalizeBaseServiceUrl(baseServiceUrl);
                boolean isGet = method.getName().equals("get");
                return newProxyInstance(SimpleRestProxyFactory.class.getClassLoader(), new Class[]{serviceClass}, new HttpMethodHandler(isGet, serviceClass, normalizedBaseServiceUrl));
            }
        });
    }

    // private helpers

    private String classNameToLowerCase(Class<?> clazz) {
        String simpleName = clazz.getSimpleName();
        return toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }

    private String normalizeBaseServiceUrl(String baseServiceUrl) {
        return baseServiceUrl.
                replaceFirst("/+$", "");
    }

    // private classes

    private class HttpMethodHandler implements InvocationHandler {

        private final boolean isGet;

        private final Class<?> serviceClass;

        private final String baseServiceUrl;

        private HttpMethodHandler(boolean isGet, Class<?> serviceClass, String baseServiceUrl) {
            this.isGet = isGet;
            this.serviceClass = serviceClass;
            this.baseServiceUrl = baseServiceUrl;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Class<?> returnType = method.getReturnType();
            String url = baseServiceUrl + "/" + classNameToLowerCase(serviceClass) + "/" + method.getName();
            int argumentsInUri = isGet ? args.length : args.length - 1;
            for (int i = 0; i < argumentsInUri; i++) {
                url += "/" + args[i].toString();
            }
            if(isGet) {
                return restOperations.getForObject(url, returnType);
            } else {
                Object request = args[args.length - 1];
                return restOperations.postForObject(url, request, returnType);
            }
        }
    }

}
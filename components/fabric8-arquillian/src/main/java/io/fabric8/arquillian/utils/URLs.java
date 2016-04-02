/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.arquillian.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.concurrent.Callable;

/**
 */
public class URLs {
    private static final transient Logger LOG = LoggerFactory.getLogger(URLs.class);


    /**
     * Executes the given block ensuring that the Maven URL handler is properly registered,
     * whatever is happening with system properties.
     */
    public static <T> T doWithMavenURLHandlerFactory(Callable<T> block) throws Exception {
        URLStreamHandlerFactory factory = new URLStreamHandlerFactory() {
            @Override
            public URLStreamHandler createURLStreamHandler(String protocol) {
                if (protocol.equals("mvn")) {
                    return new org.ops4j.pax.url.mvn.Handler();
                }
                return null;
            }
        };
        return doWithCustomURLHandlerFactory(factory, block);
    }


    /**
     * Executes the given block with the given {@link URLStreamHandlerFactory},
     * whatever is happening with system properties.
     */
    public static <T> T doWithCustomURLHandlerFactory(final URLStreamHandlerFactory customFactory, Callable<T> block) throws Exception {
        final URLStreamHandlerFactory oldFactory = getURLStreamHandlerFactory();
        try {
            URLStreamHandlerFactory newFactory = new URLStreamHandlerFactory() {
                @Override
                public URLStreamHandler createURLStreamHandler(String protocol) {
                    URLStreamHandler answer = customFactory.createURLStreamHandler(protocol);
                    if (answer == null && oldFactory != null) {
                        answer = oldFactory.createURLStreamHandler(protocol);
                    }
                    return answer;
                }
            };
            setURLStreamHandlerFactory(newFactory);
            return block.call();
        } finally {
            setURLStreamHandlerFactory(oldFactory);
        }

    }

    /**
     * The code barfs if we've already set the factory so we have to clear it via reflection first.
     * <p/>
     * Hacks FTW :)
     */
    static void setURLStreamHandlerFactory(URLStreamHandlerFactory newFactory) {
        String fieldName = "factory";
        Class<URL> clazz = URL.class;
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            URLStreamHandlerFactory oldValue = (URLStreamHandlerFactory) field.get(null);
            if (oldValue != null) {
                field.set(null, null);
            }
        } catch (NoSuchFieldException e) {
            LOG.error("Could not find field " + fieldName + " in class " + clazz.getName() + ". " + e, e);
        } catch (IllegalAccessException e) {
            LOG.error("Could not access field " + fieldName + " in class " + clazz.getName() + ". " + e, e);
        }
        if (newFactory != null) {
            URL.setURLStreamHandlerFactory(newFactory);
        }
    }

    /**
     * For some odd reason this is private so lets use lovely reflection as a hack!
     */
    static URLStreamHandlerFactory getURLStreamHandlerFactory() {
        String fieldName = "factory";
        Class<URL> clazz = URL.class;
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (URLStreamHandlerFactory) field.get(null);
        } catch (NoSuchFieldException e) {
            LOG.error("Could not find field " + fieldName + " in class " + clazz.getName() + ". " + e, e);
        } catch (IllegalAccessException e) {
            LOG.error("Could not access field " + fieldName + " in class " + clazz.getName() + ". " + e, e);
        }
        return null;
    }
}

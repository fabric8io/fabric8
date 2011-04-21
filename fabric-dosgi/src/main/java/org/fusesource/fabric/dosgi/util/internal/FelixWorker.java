/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.fusesource.fabric.dosgi.util.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public final class FelixWorker extends DefaultWorker implements FrameworkUtilWorker {
    private static Method getCurrentModuleMethod;
    private static Method getClassLoader;

    static {
        Bundle b = FrameworkUtil.getBundle(FelixWorker.class);
        try {
            getCurrentModuleMethod = b.getClass().getDeclaredMethod("getCurrentModule");
            Object result = getCurrentModuleMethod.invoke(b);
            getClassLoader = result.getClass().getDeclaredMethod("getClassLoader");

            getCurrentModuleMethod.setAccessible(true);
            getClassLoader.setAccessible(true);
        } catch (SecurityException e) {
        } catch (NoSuchMethodException e) {
        } catch (IllegalArgumentException e) {
        } catch (IllegalAccessException e) {
        } catch (InvocationTargetException e) {
        }
    }

    public ClassLoader getClassLoader(Bundle b) {
        if (getCurrentModuleMethod != null) {
            try {
                Object result = getCurrentModuleMethod.invoke(b);
                if (result != null) {
                    Object cl = getClassLoader.invoke(result);

                    if (cl instanceof ClassLoader) return (ClassLoader) cl;
                }
            } catch (IllegalArgumentException e) {
            } catch (IllegalAccessException e) {
            } catch (InvocationTargetException e) {
            }
        }
        return null;
    }

    public boolean isValid() {
        return getCurrentModuleMethod != null && getClassLoader != null;
    }
}

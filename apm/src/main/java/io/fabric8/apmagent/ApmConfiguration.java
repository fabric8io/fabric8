/*
 * Copyright 2005-2014 Red Hat, Inc.
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.fabric8.apmagent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

public class ApmConfiguration implements ApmConfigurationMBean {
    final static Logger logger = LoggerFactory.getLogger(ApmConfiguration.class);
    private boolean trace = false;
    private boolean debug = false;
    private boolean asyncTransformation = false;
    private boolean startJolokiaAgent = false;
    private boolean autoStartMetrics = false;
    private boolean usePlatformMBeanServer = true;
    private boolean verifyClasses = false;
    private List<FilterItem> whiteFilterList = new ArrayList<>();
    private List<FilterItem> blackFilterList = new ArrayList<>();
    private List<ApmConfigurationFilterChangeListener> changeListeners = new CopyOnWriteArrayList<>();

    ApmConfiguration() {
        addToBlackList("java");
        addToBlackList("com.sun");
        addToBlackList("sun");
        addToBlackList("$Proxy");
        addToBlackList("ByCGLIB$$");
        addToBlackList("io.fabric8.apmagent");
        addToBlackList("org.apache.camel.spring.remoting");
        addToBlackList("org.jolokia");
        addToBlackList("org.springframework");
        addToBlackList("org.eclipse");
        addToBlackList("org.apache.xbean");
        addToBlackList("org.slf4j");
        addToBlackList("org.omg");
        addToBlackList("com.apple");
        addToBlackList("oracle");
        addToBlackList("org.apache.log4j");
        addToBlackList("org.objectweb.asm");
        addToBlackList("org.apache.commons");
        addToBlackList("org.apache.jasper");
        addToBlackList("jrockit");
        addToBlackList("org.json");
        addToBlackList("org.fusesource.hawtbuf");
        addToBlackList("com.intellij");
        addToBlackList("org.w3c.dom");
        addToBlackList("com.codahale");
        //for testing only
        addToWhiteList("io.fabric8.testApp");

    }

    @Override
    public String getWhiteList() {
        return getListAsString(whiteFilterList);
    }

    @Override
    public void setWhiteList(String whiteList) {
        whiteFilterList = new ArrayList<>();
        initializeList(whiteList, this.whiteFilterList);
        fireFilterChangeListener();
    }

    @Override
    public String getBlackList() {
        return getListAsString(blackFilterList);
    }

    @Override
    public void setBlackList(String blackList) {
        this.blackFilterList = new ArrayList<>();
        initializeList(blackList, this.blackFilterList);
        fireFilterChangeListener();
    }

    @Override
    public void addToBlackList(String s) {
        FilterItem filterItem = new FilterItem();
        String[] classAndMethod = s.split("@");
        filterItem.setClassName(classAndMethod[0]);
        if (classAndMethod.length > 1) {
            filterItem.setMethodName(classAndMethod[1]);
        }
        blackFilterList.add(filterItem);
        fireFilterChangeListener();
    }

    @Override
    public void addToWhiteList(String s) {
        FilterItem filterItem = new FilterItem();
        String[] classAndMethod = s.split("@");
        filterItem.setClassName(classAndMethod[0]);
        if (classAndMethod.length > 1) {
            filterItem.setMethodName(classAndMethod[1]);
        }
        whiteFilterList.add(filterItem);
        fireFilterChangeListener();
    }

    @Override
    public boolean isTrace() {
        return trace;
    }

    @Override
    public void setTrace(boolean trace) {
        this.trace = trace;
    }

    @Override
    public boolean isDebug() {
        return debug;
    }

    @Override
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    @Override
    public boolean isAsyncTransformation() {
        return asyncTransformation;
    }

    @Override
    public void setAsyncTransformation(boolean asyncTransformation) {
        this.asyncTransformation = asyncTransformation;
    }

    public boolean isStartJolokiaAgent() {
        return startJolokiaAgent;
    }

    public void setStartJolokiaAgent(boolean startJolokiaAgent) {
        this.startJolokiaAgent = startJolokiaAgent;
    }

    public boolean isAutoStartMetrics() {
        return autoStartMetrics;
    }

    public void setAutoStartMetrics(boolean autoStartMetrics) {
        this.autoStartMetrics = autoStartMetrics;
    }

    public boolean isUsePlatformMBeanServer() {
        return usePlatformMBeanServer;
    }

    public void setUsePlatformMBeanServer(boolean usePlatformMBeanServer) {
        this.usePlatformMBeanServer = usePlatformMBeanServer;
    }

    public boolean isVerifyClasses() {
        return verifyClasses;
    }

    public void setVerifyClasses(boolean verifyClasses) {
        this.verifyClasses = verifyClasses;
    }


    public void initalizeFromProperties(Properties properties) {
        for (Map.Entry entry : properties.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                setProperty(entry.getKey().toString(), entry.getValue().toString());
            }
        }
    }

    public boolean isAudit(String className) {
        return isWhiteListed(className) || !isBlackListed(className);
    }

    public boolean isAudit(String className, String methodName) {
        return isWhiteListed(className, methodName) || !isBlackListed(className, methodName);
    }

    public boolean isBlackListed(String className) {
        for (FilterItem item : blackFilterList) {
            if (item.matches(className)) {
                return true;
            }
        }
        return false;
    }

    public boolean isBlackListed(String className, String methodName) {
        for (FilterItem item : blackFilterList) {
            if (item.matches(className, methodName, true)) {
                return true;
            }
        }
        return false;
    }

    public boolean isWhiteListed(String className) {

        if (whiteFilterList.isEmpty())
            return false;
        for (FilterItem item : whiteFilterList) {
            if (className.matches(item.getClassName()))
                return true;
        }
        return false;
    }

    public boolean isWhiteListed(String className, String methodName) {
        for (FilterItem item : whiteFilterList) {
            if (item.matches(className, methodName, false)) {
                return true;
            }
        }
        return false;
    }

    public void addChangeListener(ApmConfigurationFilterChangeListener changeListener) {
        changeListeners.add(changeListener);
    }

    public void removeChangeListener(ApmConfigurationFilterChangeListener changeListener) {
        changeListeners.remove(changeListener);
    }

    private void fireFilterChangeListener() {
        for (ApmConfigurationFilterChangeListener apmConfigurationFilterChangeListener : this.changeListeners) {
            apmConfigurationFilterChangeListener.configurationFilterChanged();
        }
    }

    private void setProperty(String name, Object value) {
        try {
            Method setter = findSetterMethod(name);
            if (setter != null) {
                if (value == null || value.getClass().equals(setter.getParameterTypes()[0])) {
                    setter.invoke(this, value);
                } else {
                    setter.invoke(this, convert(value, setter.getParameterTypes()[0]));
                }
            }
        } catch (Throwable e) {
            logger.warn("Failed to set property: " + e.getMessage(), e);
        }
    }

    private Method findSetterMethod(String name) {
        String methodName = "set" + name.substring(0, 1).toUpperCase() + name.substring(1);
        Method[] methods = ApmConfiguration.class.getMethods();
        for (Method method : methods) {
            Class params[] = method.getParameterTypes();
            if (method.getName().equals(methodName) && params.length == 1) {
                return method;
            }
        }
        return null;
    }

    private Object convert(Object value, Class type) throws Exception {
        PropertyEditor editor = PropertyEditorManager.findEditor(type);
        if (editor != null) {
            editor.setAsText(value.toString());
            return editor.getValue();
        }
        if (type == URI.class) {
            return new URI(value.toString());
        }
        return null;
    }

    private void initializeList(String str, List<FilterItem> list) {

        String[] split = str.split(",");

        for (String s : split) {
            FilterItem filterItem = new FilterItem();
            String[] classAndMethod = s.split("@");
            filterItem.setClassName(classAndMethod[0]);
            if (classAndMethod.length > 1) {
                filterItem.setMethodName(classAndMethod[1]);
            }
            list.add(filterItem);

        }
    }

    private String getListAsString(List<FilterItem> list) {
        String result = "";
        if (list != null && !list.isEmpty()) {
            for (int i = 0; i < list.size(); i++) {
                FilterItem filterItem = list.get(i);
                result += filterItem.getClassName();
                String methodName = filterItem.getMethodName();
                if (methodName != null && methodName.length() > 0) {
                    result += "@" + methodName;
                }
                if (i < (list.size() - 1)) {
                    result += ",";
                }
            }
        }
        return result;
    }
}

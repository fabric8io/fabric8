/**
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
package io.fabric8.cxf;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.common.classloader.ClassLoaderUtils;

import java.util.ArrayList;
import java.util.List;

public class FabricFailOverFeature extends FabricLoadBalancerFeature {
    private static final transient Log LOG = LogFactory.getLog(FabricFailOverFeature.class);
    protected String exceptions;
    protected List<Class> exceptionList = new ArrayList<Class>();

    protected LoadBalanceStrategy getDefaultLoadBalanceStrategy() {
        // This strategy always return the first physical address from the locator
        return new FirstOneLoadBalanceStrategy();
    }

    protected LoadBalanceTargetSelector getDefaultLoadBalanceTargetSelector() {
        return new FailOverTargetSelector(exceptionList);
    }

    public void setExceptions(String exceptions) {
        this.exceptions = exceptions;
        if (exceptions != null) {
            String[] exceptionArray =  exceptions.split(";");
            for (String exception: exceptionArray) {
                try {
                    Class<?> clazz = ClassLoaderUtils.loadClass(exception, this.getClass());
                    exceptionList.clear();
                    exceptionList.add(clazz);
                } catch (ClassNotFoundException ex) {
                    LOG.warn("Can't load the exception " + exception + " for the FabricFailOverFeature.");
                }
            }
        }
    }

}

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fabric8.karaf.checks.internal;

import java.util.Collections;
import java.util.List;

import io.fabric8.karaf.checks.Check;
import org.osgi.framework.Constants;
import org.osgi.framework.startlevel.FrameworkStartLevel;

public class FrameworkState extends AbstractChecker {

    @Override
    protected List<Check> doCheck() {
        int bsl = Integer.parseInt(System.getProperty(Constants.FRAMEWORK_BEGINNING_STARTLEVEL));
        int sl = systemBundle.adapt(FrameworkStartLevel.class).getStartLevel();
        if (sl < bsl) {
            return Collections.singletonList(new Check("framework-state", "OSGi Framework is not fully started"));
        } else {
            return Collections.emptyList();
        }
    }

}

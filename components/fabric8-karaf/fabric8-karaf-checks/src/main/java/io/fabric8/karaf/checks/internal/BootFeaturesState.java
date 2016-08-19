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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import io.fabric8.karaf.checks.Check;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

public class BootFeaturesState extends AbstractChecker {

    @Override
    protected List<Check> doCheck() {
        for (Bundle bundle : systemBundleContext.getBundles()) {
            if ("org.apache.karaf.features.core".equals(bundle.getSymbolicName())) {
                if (!bootFeaturesInstalled(bundle)) {
                    return Collections.singletonList(new Check("boot-features-state", "Boot Features are not yet installed"));
                }
            }
        }
        return Collections.emptyList();
    }

    private boolean bootFeaturesInstalled(Bundle bundle) {
        if (bundle.getVersion().getMajor() < 4) {
            File file = bundle.getBundleContext().getDataFile("FeaturesServiceState.properties");
            if (!file.exists()) {
                return false;
            }
            Properties props = new Properties();
            try (InputStream is = new FileInputStream(file)) {
                props.load(is);
            } catch (IOException e) {
                return false;
            }
            return Boolean.parseBoolean((String) props.get("bootFeaturesInstalled"));
        } else {
            ServiceReference<?> ref = bundleContext.getServiceReference("org.apache.karaf.features.BootFinished");
            return ref != null;
        }
    }

}

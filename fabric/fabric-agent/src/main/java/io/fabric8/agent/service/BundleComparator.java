/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.agent.service;

import java.util.Comparator;

import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

public class BundleComparator implements Comparator<Bundle> {

    @Override
    public int compare(Bundle o1, Bundle o2) {
        String bsn1 = o1.getSymbolicName();
        String bsn2 = o2.getSymbolicName();
        int c = bsn1.compareTo(bsn2);
        if (c == 0) {
            Version v1 = o1.getVersion();
            Version v2 = o2.getVersion();
            c = v1.compareTo(v2);
            if (c == 0) {
                c = o1.hashCode() - o2.hashCode();
            }
        }
        return c;
    }

}

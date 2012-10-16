/*
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
package org.apache.directory.shared.ldap.schema.ldif.extractor.impl;

import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class ResourceMap {

    public static Map<String,Boolean> getResources( Pattern pattern ) {
        Bundle bundle = FrameworkUtil.getBundle(ResourceMap.class);
        String base = bundle.getResource("META-INF/MANIFEST.MF").toExternalForm();
        int prefix = base.indexOf("META-INF/MANIFEST.MF");
        Map<String, Boolean> map = new HashMap<String, Boolean>();
        for (Enumeration e = bundle.findEntries("", "*", true); e.hasMoreElements();) {
            String url = ((URL) e.nextElement()).toExternalForm();
            url = url.substring(prefix);
            if (pattern.matcher(url).matches()) {
                map.put(url, true);
            }
        }
        return map;
    }

}

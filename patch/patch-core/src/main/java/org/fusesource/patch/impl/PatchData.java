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
package org.fusesource.patch.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class PatchData {

    private static final String ID = "id";
    private static final String DESCRIPTION = "description";
    private static final String BUNDLES = "bundle";
    private static final String COUNT = "count";
    private static final String RANGE = "range";

    private final String id;
    private final String description;
    private final Collection<String> bundles;
    private final Map<String, String> versionRanges;

    public PatchData(String id, String description, Collection<String> bundles, Map<String, String> versionRanges) {
        this.id = id;
        this.description = description;
        this.bundles = bundles;
        this.versionRanges = versionRanges;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public String getVersionRange(String bundle) {
        return versionRanges.get(bundle);
    }

    public Collection<String> getBundles() {
        return bundles;
    }

    public static PatchData load(InputStream is) throws IOException {
        Properties props = new Properties();
        props.load(is);
        String id = props.getProperty(ID);
        String desc = props.getProperty(DESCRIPTION);
        List<String> bundles = new ArrayList<String>();
        Map<String, String> ranges = new HashMap<String, String>();
        int count = Integer.parseInt(props.getProperty(BUNDLES + "." + COUNT, "0"));
        for (int i = 0; i < count; i++) {
            String key = BUNDLES + "." + Integer.toString(i);
            String bundle = props.getProperty(key);
            bundles.add(bundle);

            if (props.containsKey(key + "." + RANGE)) {
                ranges.put(bundle, props.getProperty(key + "." + RANGE));
            }
        }
        return new PatchData(id, desc, bundles, ranges);
    }

}

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
package io.fabric8.agent.service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.felix.utils.version.VersionRange;
import org.apache.felix.utils.version.VersionTable;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

import static java.util.jar.JarFile.MANIFEST_NAME;

public class MetadataBuilder {

    private final Map<String, Map<VersionRange, Map<String, String>>> metadata;

    public MetadataBuilder(Map<String, Map<VersionRange, Map<String, String>>> metadata) {
        this.metadata = metadata;
    }

    public Map<String, String> getMetadata(String url, File file) throws IOException {
        try (
                InputStream is = new BufferedInputStream(new FileInputStream(file))
        ) {
            ZipInputStream zis = new ZipInputStream(is);
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (MANIFEST_NAME.equals(entry.getName())) {
                    Attributes attributes = new Manifest(zis).getMainAttributes();
                    Map<String, String> headers = new HashMap<String, String>();
                    for (Map.Entry attr : attributes.entrySet()) {
                        headers.put(attr.getKey().toString(), attr.getValue().toString());
                    }
                    return overrideHeaders(headers);
                }
            }
        }
        throw new IllegalArgumentException("Resource " + url + " does not contain a manifest");
    }

    public Map<String, String> overrideHeaders(Map<String, String> headers) {
        String bsn = headers.get(Constants.BUNDLE_SYMBOLICNAME);
        String vstr = headers.get(Constants.BUNDLE_VERSION);
        if (bsn != null && vstr != null) {
            if (bsn.indexOf(';') > 0) {
                bsn = bsn.substring(0, bsn.indexOf(';'));
            }
            Version ver = VersionTable.getVersion(vstr);
            Map<VersionRange, Map<String, String>> ranges = metadata != null ? metadata.get(bsn) : null;
            if (ranges != null) {
                for (Map.Entry<VersionRange, Map<String, String>> entry2 : ranges.entrySet()) {
                    if (entry2.getKey().contains(ver)) {
                        for (Map.Entry<String, String> entry3 : entry2.getValue().entrySet()) {
                            String val;
                            if (entry3.getValue().startsWith("=")) {
                                val = entry3.getValue().substring(1);
                            } else {
                                val = headers.get(entry3.getKey());
                                if (val != null) {
                                    val += "," + entry3.getValue();
                                } else {
                                    val = entry3.getValue();
                                }
                            }
                            headers.put(entry3.getKey(), val);
                        }
                    }
                }
            }
        }
        return headers;
    }

}

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
package io.fabric8.zookeeper.utils;

import io.fabric8.common.util.Closeables;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class RegexSupport {

//    public static final String PROFILE_REGEX = "/fabric/configs/versions/[\\w\\.\\-]*/profiles/[\\w\\.\\-]*";
    public static final String METADATA_REGEX = "/fabric/import/fabric/registry/containers/config/[\\w\\.\\-]*/metadata";
//    public static final String PROFILE_CONTAINER_PROPERTIES_REGEX = "/fabric/configs/versions/[\\w\\.\\-]*/profiles/[\\w\\.\\-]*/io.fabric8.agent.properties";
//    public static final String PROFILE_ATTRIBUTES_REGEX = "/fabric/configs/versions/[\\w\\.\\-]*/profiles/[\\w\\.\\-]*/attributes.properties";
//    public static final String PARENTS_REGEX = "parents=[[\\w\\-\\.]*[ \\t]]*";
//    public static final String PROFILE_REGEX_FORMAT = "/fabric/configs/versions/[\\w\\.\\-]*/profiles/%s/[^ ]*";
//    public static final String VERSION_REGEX_FORMAT = "/fabric/configs/versions/%s/[^ ]*";
//    public static final String VERSION_PROFILE_REGEX_FORMAT = "/fabric/configs/versions/%s/profiles/%s/[^ ]*";

    private RegexSupport() {
        //Utility Class
    }

    public static String[] merge(File file, String[] regex) throws Exception {
        ArrayList<String> list = new ArrayList<String>();
        if (regex != null) {
            for (String r : regex) {
                list.add(r);
            }
        }

        if (file.exists() && file.isFile()) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(file));
                String s = reader.readLine();
                while (s != null) {
                    list.add(s);
                    s = reader.readLine();
                }
            } catch (Exception e) {
                throw new RuntimeException("Error reading from " + file + " : " + e);
            } finally {
                Closeables.closeQuitely(reader);
            }
        }

        String rc[] = new String[list.size()];
        list.toArray(rc);
        return rc;
    }

    public static boolean matches(List<Pattern> patterns, String value, boolean defaultOnEmpty) {
        if ( patterns.isEmpty() ) {
            return defaultOnEmpty;
        }
        boolean rc = false;
        for ( Pattern pattern : patterns ) {
            if ( pattern.matcher(value).matches() ) {
                rc = true;
            }
        }
        return rc;
    }

    public static List<Pattern> getPatterns(String[] regex) {
        List<Pattern> patterns = new ArrayList<Pattern>();
        if ( regex != null ) {
            for ( String p : regex ) {
                patterns.add(Pattern.compile(p));
            }
        }
        return patterns;
    }

}

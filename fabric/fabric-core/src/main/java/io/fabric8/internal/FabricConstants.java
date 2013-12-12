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

package io.fabric8.internal;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

public final class FabricConstants {

    private FabricConstants() {
        //Utility Class
    }

    public static final String FABRIC_VERSION;
    public static final String FRAMEWORK_VERSION;

    static {
        String fabricVersion="unknown";
        String frameworkVersion="unknown";
        InputStream is = FabricConstants.class.getResourceAsStream("version.properties");
        InputStreamReader reader = null;
        Properties properties = new Properties();

        try {
            reader = new InputStreamReader(is, "UTF-8");
            properties.load(reader);
            fabricVersion = (String) properties.get("FABRIC_VERSION");
            frameworkVersion = (String) properties.get("FRAMEWORK_VERSION");
        } catch (Throwable e) {
        } finally {
            try {
                reader.close();
            } catch (Throwable e) {
            }
          try {
            is.close();
          } catch (Throwable e) {
          }

        }
        FABRIC_VERSION = fabricVersion;
        FRAMEWORK_VERSION = frameworkVersion;
    }
}

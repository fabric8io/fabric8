/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.project.support;

import org.omg.CORBA.DynAnyPackage.Invalid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 */
public class IniFileUtils {
    private static final transient Logger LOG = LoggerFactory.getLogger(IniFileUtils.class);

    /**
     * Returns the INI file, such as ~/.gitconfig parsed
     * @param file
     */
    public static Map<String, Properties> parseIniFile(File file) throws IOException {
        Map<String, Properties> answer = new HashMap<>();
        String section = null;
        Properties properties = new Properties();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                if (line.trim().length() == 0) {
                    continue;
                }
                if (line.startsWith("[")) {
                    line = line.trim();
                    if (!line.endsWith("]")) {
                        LOG.warn("Invalid section header in file: " + file + ". Line: " + line);
                        continue;
                    }
                    section = line.substring(1, line.length() - 1);
                    properties = answer.get(section);
                    if (properties == null) {
                        properties = new Properties();
                        answer.put(section, properties);
                    }
                } else {
                    String[] parts = line.split("=", 2);
                    if (parts == null && parts.length != 2) {
                        LOG.warn("Invalid property in section: " + section + " file: " + file + ". Line: " + line);
                        continue;
                    }
                    String key = parts[0].trim();
                    String value = parts[1].trim();
                    properties.setProperty(key, value);
                }
            }
        }
        return answer;
    }
}

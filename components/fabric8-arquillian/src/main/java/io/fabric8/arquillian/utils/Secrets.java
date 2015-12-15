/**
 *  Copyright 2005-2015 Red Hat, Inc.
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
package io.fabric8.arquillian.utils;

import io.fabric8.utils.Strings;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Secrets {

    public static final String FOLDER_GROUP = "folder";
    public static final String CONTENT_GROUP = "content";
    public static final String NAME_REGEX = "[a-zA-Z0-9\\-_.]+";
    public static final String FOLDER_REGEX = "(?<" + FOLDER_GROUP + ">" + NAME_REGEX + ")" + "(\\[(?<" + CONTENT_GROUP + ">(" + NAME_REGEX + "[ ,]*)*)\\]){0,1}";
    public static final Pattern FOLDER_PATTERN = Pattern.compile(FOLDER_REGEX);

    public static List<String> getNames(String str) {
        Matcher matcher = FOLDER_PATTERN.matcher(str);
        List<String> result = new ArrayList<>();
        while (matcher.find()) {
            result.add(matcher.group(FOLDER_GROUP));
        }
        return result;
    }

    public static List<String> getContents(String str, String name) {
        List<String> result = new ArrayList<>();
        Matcher matcher = FOLDER_PATTERN.matcher(str);
        while (matcher.find()) {
            String candidate = matcher.group(FOLDER_GROUP);
            if (name.equals(candidate)) {
                String content = matcher.group(CONTENT_GROUP);
                if (Strings.isNotBlank(content)) {
                    for (String s : content.split("[ ,]+")) {
                        result.add(s);
                    }
                }
            }
        }
        return result;
    }
}

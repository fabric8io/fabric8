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
package io.fabric8.forge.addon.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class LineNumberHelper {

    public static List<String> readLines(InputStream is) throws IOException {
        List<String> answer = new ArrayList<String>();

        LineNumberReader reader = new LineNumberReader(new InputStreamReader(is));
        String line = reader.readLine();
        while (line != null) {
            answer.add(line);
            line = reader.readLine();
        }

        return answer;
    }

    public static List<String> readLines(Reader in) throws IOException {
        List<String> answer = new ArrayList<String>();

        LineNumberReader reader = new LineNumberReader(in);
        String line = reader.readLine();
        while (line != null) {
            answer.add(line);
            line = reader.readLine();
        }

        return answer;
    }

    public static String padString(String s, int spaces) {
        StringBuilder sb = new StringBuilder(s);
        int i = 0;
        while (i < spaces) {
            sb.insert(0, ' ');
            i++;
        }
        return sb.toString();
    }

    public static String linesToString(List<String> lines) {
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line);
            sb.append(System.lineSeparator());
        }
        return sb.toString();
    }

    public static int leadingSpaces(List<String> lines, int index) {
        int spaces = 0;
        // get the last indent used so we can use same indent
        for (int i = 0; i <= index; i++) {
            String line = lines.get(i);
            if (!line.isEmpty()) {
                spaces = 0;
                while (line.charAt(spaces) == ' ' && spaces < line.length()) {
                    spaces++;
                }
            }
        }
        return spaces;
    }
}

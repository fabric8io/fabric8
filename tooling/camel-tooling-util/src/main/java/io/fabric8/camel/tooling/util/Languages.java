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
package io.fabric8.camel.tooling.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Languages {

    public static List<Language> languages = Arrays.asList(
        // No longer supported by Camel
//        new Language("beanshell", "BeanShell", "BeanShell expression"),
        new Language("constant", "Constant", "Constant expression"),
        new Language("el", "EL", "Unified expression language from JSP / JSTL / JSF"),
        new Language("header", "Header", "Header value"),
        new Language("javaScript", "JavaScript", "JavaScript expression"),
        new Language("jxpath", "JXPath", "JXPath expression"),
        new Language("method", "Method", "Method call expression"),
        new Language("mvel", "MVEL", "MVEL expression"),
        new Language("ognl", "OGNL", "OGNL expression"),
        new Language("groovy", "Groovy", "Groovy expression"),
        new Language("property", "Property", "Property value"),
        new Language("python", "Python", "Python expression"),
        new Language("php", "PHP", "PHP expression"),
        new Language("ref", "Ref", "Reference to a bean expression"),
        new Language("ruby", "Ruby", "Ruby expression"),
        new Language("simple", "Simple", "Simple expression language from Camel"),
        new Language("spel", "Spring EL", "Spring expression language"),
        new Language("sql", "SQL", "SQL expression"),
        new Language("tokenize", "Tokenizer", "Tokenizing expression"),
        new Language("xpath", "XPath", "XPath expression"),
        new Language("xquery", "XQuery", "XQuery expression")
    );

    public static String[] languageArray() {
        List<String> answer = new ArrayList<String>(languages.size());
        for (Language l : languages) {
            answer.add(l.getId());
        }
        return answer.toArray(new String[languages.size()]);
    }

    public static String[][] nameAndLanguageArray() {
        List<String[]> answer = new ArrayList<String[]>(languages.size());
        for (Language l : languages) {
            answer.add(new String[] { l.getName(), l.getId() });
        }
        return answer.toArray(new String[languages.size()][]);
    }

}

/**
 *  Copyright 2005-2016 Red Hat, Inc.
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
package io.fabric8.tools.apt;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.util.Elements;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 */
public class JavaDocs {
    /**
     * Lets search the javadoc for the text "@param parameterName text (@param|@return|@exception)" and extract the text for a parameter name
     */
    public static String findParameterJavaDoc(String javadoc, String parameterName) {
        Pattern regex = Pattern.compile(".*\\s+@param\\s+" + parameterName + "\\s+(.*)");
        Matcher matcher = regex.matcher(javadoc);
        if (matcher.find()) {
            String prefix = matcher.group(1);
            if (!Strings.isNullOrEmpty(prefix)) {
                // now lets strip any trailing tags from the end of the text
                Pattern nextTag = Pattern.compile("\\s+(@param|@return|@exception|@throws|@serialData|@see|@since|@deprecated)");
                Matcher endMatcher = nextTag.matcher(prefix);
                if (endMatcher.find()) {
                    int start = endMatcher.start();
                    return prefix.substring(0, start).trim();
                } else {
                    return prefix;
                }
            }
        }
        return null;
    }

    public static String getJavaDoc(Elements elementUtils, Element element) {
        // TODO folks could maybe use an annotation to document injection parameters rather than messing with javadoc?
        if (elementUtils != null) {
            String description = elementUtils.getDocComment(element);
            if (Strings.isNullOrEmpty(description) && element.getKind() == ElementKind.PARAMETER) {
                String parentDoc = getJavaDoc(elementUtils, element.getEnclosingElement());
                if (!Strings.isNullOrEmpty(parentDoc)) {
                    return findParameterJavaDoc(parentDoc, element.getSimpleName().toString());
                }
            }
            return description;
        }
        return  null;
    }
}

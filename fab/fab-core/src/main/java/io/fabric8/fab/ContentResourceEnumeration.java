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
package io.fabric8.fab;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;

/**
*/
public class ContentResourceEnumeration implements Enumeration<URL> {
    final String name;
    Iterator<Content> iter;
    URL next;

    ContentResourceEnumeration(String name, Iterator<Content> iter) {
        this.name = name;
        this.iter = iter;
        findNext();
    }

    public boolean hasMoreElements() {
        return next != null;
    }

    public URL nextElement() {
        URL n = next;
        findNext();
        return n;
    }

    protected void findNext() {
        next = null;
        while (next == null && iter.hasNext()) {
            Content content = iter.next();
            try {
                URL e = content.getURL(name);
                if (e != null) {
                    next = e;
                }
            } catch (MalformedURLException e) {
                // Ignore
            }
        }
    }
}

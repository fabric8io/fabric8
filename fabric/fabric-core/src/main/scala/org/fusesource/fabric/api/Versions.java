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
package org.fusesource.fabric.api;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper methods for working with HasId instances
 */

// TODO we could replace this class with Ids if we refactor Version to implement HasId
public final class Versions {

    private Versions() {
        //Utility Class
    }

    public static List<String> getIds(Version[] versions) {
        List<String> answer = new ArrayList<String>();
        if (versions != null) {
            for (Version version : versions) {
                String id = version.getName();
                if (id != null) {
                    answer.add(id);
                }
            }
        }
        return answer;
    }

    public static String getId(Version version) {
        return version != null ? version.getName() : null;
    }
}

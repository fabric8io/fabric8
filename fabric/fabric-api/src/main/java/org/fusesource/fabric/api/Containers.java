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
 * A helper class for working with containers
 */
public class Containers {

    public static List<Container> containersForProfile(Container[] containers, String profileId) {
        List<Container> answer = new ArrayList<Container>();
        if (profileId != null) {
            for (Container c : containers) {
                for (Profile p : c.getProfiles()) {
                    if (profileId.equals(p.getId())) {
                        answer.add(c);
                    }
                }
            }
        }
        return answer;
    }
}

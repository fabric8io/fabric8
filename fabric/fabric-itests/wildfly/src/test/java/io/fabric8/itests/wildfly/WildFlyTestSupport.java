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

package io.fabric8.itests.wildfly;

import java.util.concurrent.TimeUnit;

import io.fabric8.api.Container;
import io.fabric8.api.Profile;
import io.fabric8.itests.paxexam.support.FabricTestSupport;

public class WildFlyTestSupport extends FabricTestSupport {


	protected Profile getProfile(Container container, String name) throws Exception {
		return getProfile(container, name, DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
	}

	protected Profile getProfile(Container container, String name, long timeout, TimeUnit unit) throws Exception {
		Profile result = null;
        for (long t = 0;  result == null && t < unit.toMillis(timeout); t += 200) {
			Profile[] profiles = container.getProfiles();
			for (Profile aux : profiles) {
				if (name.equals(aux.getId())) {
					result = aux;
					break;
				}
			}
            Thread.sleep(200);
        }
		return result;
	}
}


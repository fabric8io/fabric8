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
package org.fusesource.fabric.git.hawtio;

import io.hawt.introspect.Introspector;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;

@Component(name = "org.fusesource.fabric.hawtio.introspector", description = "Fabric Hawtio Introspector Service", immediate = true)
@Service(Introspector.class)
public class FabricIntrospector extends Introspector {

    @Activate
    @Override
    public void init() throws Exception {
        super.init();
    }

    @Deactivate
    @Override
    public void destroy() throws Exception {
        super.destroy();
    }
}

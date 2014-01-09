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
package io.fabric8.jaxb.dynamic.profile;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import io.fabric8.jaxb.dynamic.DynamicCompiler;
import io.fabric8.jaxb.dynamic.DynamicJaxbDataFormat;

/**
 * A Declarative Services implementation of {@link DynamicJaxbDataFormat} which will be injected
 * with the required {@link io.fabric8.jaxb.dynamic.DynamicCompiler}
 */
@Component(name = "io.fabric8.profile.jaxb.context", label = "Fabric8 Profile JAXB Context", immediate = true, metatype = false)
@Service({DynamicJaxbDataFormat.class})
public class ProfileJaxbDataFormat extends DynamicJaxbDataFormat {

    @Reference(referenceInterface = DynamicCompiler.class,
            bind = "bindCompiler")
    private DynamicCompiler compiler;

    public void bindCompiler(DynamicCompiler value) throws Exception {
        setCompiler(value);
    }

}

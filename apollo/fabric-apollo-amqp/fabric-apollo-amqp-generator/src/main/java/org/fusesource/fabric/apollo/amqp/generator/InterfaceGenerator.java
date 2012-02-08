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

package org.fusesource.fabric.apollo.amqp.generator;

import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JDefinedClass;

import java.io.IOException;

import static com.sun.codemodel.ClassType.INTERFACE;

public class InterfaceGenerator {
    private final Generator generator;

    public InterfaceGenerator(Generator generator) {
        this.generator = generator;
    }

    void generateAbstractBases() throws JClassAlreadyExistsException, IOException {
        for ( String base : generator.getProvides() ) {
            String pkg = generator.getInterfaces() + ".";
            String name = pkg + Utilities.toJavaClassName(base);
            Log.info("generating interface with name %s", name);
            JDefinedClass cls = generator.getCm()._class(name, INTERFACE);
            cls._implements(generator.getCm().ref(generator.getAmqpBaseType()));
        }
    }
}
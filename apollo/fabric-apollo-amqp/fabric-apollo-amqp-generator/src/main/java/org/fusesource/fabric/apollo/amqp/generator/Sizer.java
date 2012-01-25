/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.fabric.apollo.amqp.generator;

import com.sun.codemodel.*;

/**
 *
 */
public class Sizer {

    JDefinedClass definedClass;

    JCodeModel cm;
    Generator generator;

    public Sizer(Generator generator, String className) throws JClassAlreadyExistsException {
        cm = generator.getCm();
        this.generator = generator;

        definedClass = cm._class(className, ClassType.INTERFACE);

        generator.registry().cls().field(JMod.PROTECTED | JMod.FINAL | JMod.STATIC, cls(), "SIZER", JExpr.direct("AMQPSizer.instance()"));
        JMethod singletonAccessor = generator.registry().cls().method(JMod.PUBLIC, cls(), "sizer");
        singletonAccessor.body()._return(JExpr.ref("SIZER"));

    }

    public JDefinedClass cls() {
        return definedClass;
    }
}

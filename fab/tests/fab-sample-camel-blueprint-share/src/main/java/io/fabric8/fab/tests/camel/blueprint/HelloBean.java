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
package io.fabric8.fab.tests.camel.blueprint;

import org.apache.camel.Message;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A bean which we use in the route
 */
public class HelloBean  {

    private String say = "Hello World";

    public String hello(Message message) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return say + " at " + sdf.format(new Date()) + " with headers " + message.getHeaders();
    }

    public String getSay() {
        return say;
    }

    public void setSay(String say) {
        this.say = say;
    }
}

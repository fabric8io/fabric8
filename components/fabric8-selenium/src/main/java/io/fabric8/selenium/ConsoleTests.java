/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.selenium;

import io.fabric8.utils.Millis;
import org.openqa.selenium.By;

/**
 * Helper methods for tests using the fabric8 console
 */
public class ConsoleTests {
    public static void waitUntilLoggedIn(final WebDriverFacade facade, String namespace) {
        facade.form().
                clearAndSendKeys(By.id("inputUsername"), "admin").
                clearAndSendKeys(By.id("inputPassword"), "admin").
                submit();

        facade.logInfo("Logged in - waiting for the browser initialise the web app");
        facade.sleep(Millis.seconds(5));
        facade.logInfo("Logged in!");

        // now lets switch to the default namespace
        By namespaceSelectBy = By.xpath("//select[@ng-model='namespace']");
        facade.untilIsEnabled(namespaceSelectBy);
        facade.untilSelectedByVisibleText(namespaceSelectBy, namespace);

        facade.sleep(Millis.seconds(2));
        facade.logInfo("Viewing namespace: " + namespace);


    }

}

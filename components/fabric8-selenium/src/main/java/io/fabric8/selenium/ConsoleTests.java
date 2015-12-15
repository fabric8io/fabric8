/**
 *  Copyright 2005-2015 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.selenium;

import io.fabric8.utils.Millis;
import org.openqa.selenium.By;

/**
 * Helper methods for tests using the fabric8 console
 */
public class ConsoleTests {
    protected static final By inputUsernameBy = By.id("inputUsername");
    protected static final By inputPasswordBy = By.id("inputPassword");
    protected static final By namespaceSelectBy = By.xpath("//select[@ng-model='namespace']");

    public static void waitUntilLoggedIn(final WebDriverFacade facade) {
        waitUntilLoggedIn(facade, "admin", "admin");
    }

    public static void waitUntilLoggedIn(WebDriverFacade facade, String userName, String password) {
        String namespace = facade.getNamespace();

        facade.sleep(Millis.seconds(5));

        // lets retry a few times in case the first navigation doesn't work properly
        for (int i = 0; i < 5; i++) {
            try {
                facade.untilIsDisplayed(inputUsernameBy);
            } catch (Throwable e) {
                // lets try reload the browser?
                facade.logWarn("Trying to reload the browser!");
                facade.getDriver().navigate().refresh();
            }
        }

        facade.form().
                clearAndSendKeys(inputUsernameBy, userName).
                clearAndSendKeys(inputPasswordBy, password).
                submit();

        facade.logInfo("Logged in - waiting for the browser initialise the web app");
        facade.sleep(Millis.seconds(5));
        facade.logInfo("Logged in!");

        // now lets switch to the default namespace
        for (int i = 0; i < 5; i++) {
            try {
                facade.untilIsEnabled(namespaceSelectBy);
            } catch (Throwable e) {
                // lets try reload the browser?
                facade.logWarn("Trying to reload the browser!");
                facade.getDriver().navigate().refresh();
            }
        }

        facade.untilSelectedByVisibleText(namespaceSelectBy, namespace);

        facade.sleep(Millis.seconds(10));
        facade.logInfo("Viewing namespace: " + namespace);
    }

}

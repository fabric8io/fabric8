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
package io.fabric8.selenium.inputs;

import io.fabric8.selenium.PageSupport;
import io.fabric8.selenium.WebDriverFacade;
import io.fabric8.utils.Millis;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;


/**
 * Stores the {@link By} and value of an input field for a form
 */
public class InputValue extends PageSupport {
    private final By by;
    private final String value;

    public InputValue(WebDriverFacade facade, By by, String value) {
        super(facade);
        this.by = by;
        this.value = value;
    }

    @Override
    public String toString() {
        return "InputValue{" +
                "by=" + by +
                ", value='" + value + '\'' +
                '}';
    }

    public By getBy() {
        return by;
    }

    public String getValue() {
        return value;
    }


    public WebElement doInput() {
        WebElement element = getFacade().findOptionalElement(by);
        if (element != null) {
            for (int i = 0; i < 10; i++) {
                try {
                    doInputOnElement(element);
                    return element;
                } catch (StaleElementReferenceException e) {
                    logWarn("Caught: " + e);
                    getFacade().sleep(Millis.seconds(5));
                }
            }
            logWarn("Failed to perform input on " + by + " to due repeated StaleElementReferenceException!");
        }
        return null;
    }

    protected void doInputOnElement(WebElement element) {
        element.clear();
        element.sendKeys(value);
    }
}

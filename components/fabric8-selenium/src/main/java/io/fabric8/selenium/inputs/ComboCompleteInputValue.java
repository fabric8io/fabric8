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

import io.fabric8.selenium.WebDriverFacade;
import io.fabric8.utils.Millis;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

/**
 * Used to input values into a completion based combo list where we enter text; pause for the smart
 * completion list to pop up and then tab out of the field to forge the selection
 */
public class ComboCompleteInputValue extends InputValue {
    public ComboCompleteInputValue(WebDriverFacade facade, By by, String value) {
        super(facade, by, value);
    }

    @Override
    public String toString() {
        return "ComboCompleteInputValue{" +
                "by=" + getBy() +
                ", value='" + getValue() + '\'' +
                '}';
    }

    @Override
    public WebElement doInput() {
        final WebDriverFacade facade = getFacade();
        final By firstBy = getBy();

        facade.sleep(Millis.seconds(2));
        WebElement element = facade.findOptionalElement(firstBy);
        if (element == null) {
            return null;
        }
        super.doInputOnElement(element);
        logInput("" + firstBy + " value: " + getValue());

        facade.sleep(Millis.seconds(2));

        element = facade.findOptionalElement(firstBy);
        if (element != null) {
            element.sendKeys(Keys.TAB);
            facade.sleep(Millis.seconds(2));
            element = facade.findOptionalElement(firstBy);
        }
        return element;
    }
}

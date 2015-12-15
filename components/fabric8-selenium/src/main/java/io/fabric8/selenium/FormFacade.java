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

import io.fabric8.selenium.inputs.ComboCompleteInputValue;
import io.fabric8.selenium.inputs.InputValue;
import io.fabric8.utils.Millis;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.fail;

/**
 * A helper facade for submitting forms
 */
public class FormFacade extends PageSupport {
    private List<InputValue> inputValues = new ArrayList<>();
    private By submitBy;

    public FormFacade(WebDriverFacade facade) {
        super(facade);
    }

    public FormFacade clearAndSendKeys(By by, String value) {
        inputValues.add(new InputValue(getFacade(), by, value));
        return this;
    }

    public FormFacade completeComboBox(By by, String value) {
        inputValues.add(new ComboCompleteInputValue(getFacade(), by, value));
        return this;
    }

    public FormFacade submitButton(By submitBy) {
        this.submitBy = submitBy;
        return this;
    }

    public void submit() {
        getFacade().until("Form inputs: " + inputValues, new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                logWait("" + inputValues + " on " + driver.getCurrentUrl());
                WebElement submitElement = null;
                for (InputValue inputValue : inputValues) {
                    submitElement = inputValue.doInput();
                    if (submitElement == null) {
                        logInfo("Missing " + inputValue + "");
                        return false;
                    }
                }
                if (submitBy == null && submitElement == null) {
                    fail("No input fields submitted yet");
                    return false;
                } else {
                    getFacade().sleep(Millis.seconds(5));
                    if (submitBy != null) {
                        getFacade().untilIsEnabled(submitBy);
                        submitElement = getFacade().findOptionalElement(submitBy);
                        if (submitElement == null) {
                            logWarn("Could not find submit button " + submitBy + "");
                            return false;
                        } else {
                            if (!submitElement.isDisplayed() || !submitElement.isEnabled()) {
                                logWarn("Submit button " + submitBy + " not enabled and visible");
                                return false;
                            }
                            logInfo("Submitting form: " + inputValues + " on " + submitElement + "");
                            submitElement.click();
                        }
                    } else {
                        logInfo("Submitting form: " + inputValues + " on " + submitElement + "");
                        submitElement.submit();
                    }
                    return true;
                }
            }
        });
    }
}

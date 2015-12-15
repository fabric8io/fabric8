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

import org.openqa.selenium.WebDriver;

/**
 * Abstract base class to provide reusable helper methods for {@link PageSupport} and {@link WebDriverFacade}
 */
public abstract class LogSupport {
    private final WebDriver driver;

    public LogSupport(WebDriver driver) {
        this.driver = driver;
    }

    public WebDriver getDriver() {
        return driver;
    }

    public abstract WebDriverFacade getFacade();


    public void logError(String message, Throwable e) {
        SeleniumTests.logError(message + locationText(), e);
    }

    public void logWait(String message) {
        SeleniumTests.logWait(message + locationText());

    }

    public void logInput(String message) {
        SeleniumTests.logInput(message + locationText());
    }

    public void logClick(String message) {
        SeleniumTests.logClick(message + locationText());
    }

    public void logSubmit(String message) {
        SeleniumTests.logSubmit(message + locationText());
    }

    public void logInfo(String message) {
        SeleniumTests.logInfo(message + locationText());
    }

    public void logWarn(String message) {
        SeleniumTests.logWarn(message + locationText());
    }

    public void logWarn(String message, Throwable e) {
        SeleniumTests.logWarn(message + locationText(), e);
    }

    protected String locationText() {
        return " at " + getDriver().getCurrentUrl();
    }

}

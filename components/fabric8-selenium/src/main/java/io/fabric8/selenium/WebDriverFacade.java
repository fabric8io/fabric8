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

import com.google.common.base.Function;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.utils.Millis;
import io.fabric8.utils.Strings;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * A facade and helper methods for writing {@link WebDriver} based Selenium tests
 */
public class WebDriverFacade extends LogSupport {
    private final WebDriver driver;
    private final KubernetesClient client;
    private final String namespace;
    private long defaultTimeoutInSeconds = 60;

    public WebDriverFacade(WebDriver driver, KubernetesClient client, String namespace) {
        super(driver);
        this.driver = driver;
        this.client = client;
        this.namespace = namespace;
    }

    @Override
    public WebDriverFacade getFacade() {
        return this;
    }

    public String getNamespace() {
        return namespace;
    }

    public KubernetesClient getClient() {
        return client;
    }

    /**
     * Returns the service URL for the given service name
     */
    public String getServiceUrl(String serviceName) {
        String url = KubernetesHelper.getServiceURL(client, serviceName, namespace, "http", true);
        assertNotNull("No external Service URL could be found for namespace: " + namespace + " and name: " + serviceName, url);
        assertTrue("No external Service URL could be found for namespace: " + namespace + " and name: " + serviceName, Strings.isNotBlank(url));
        logInfo("Service " + serviceName + " in namespace: " + namespace + " URL = " + url);
        return url;
    }


    /**
     * Navigates to the given service name in the current namespace
     *
     * @return the URL navigated to
     */
    public String navigateToService(String serviceName) {
        String url = getServiceUrl(serviceName);
        WebDriver driver = getDriver();
        driver.navigate().to(url);
        return url;
    }

    /**
     * Finds an element or returns null if it could not be found
     */
    public WebElement findOptionalElement(By by) {
        try {
            return getDriver().findElement(by);
        } catch (NoSuchElementException e) {
            return null;
        } catch (Throwable e) {
            logError("Failed to find " + by, e);
            return null;
        }
    }

    /**
     * Find an optinoal element from a given element or return null
     *
     * @param element
     * @param by
     */
    public WebElement findOptionalElement(WebElement element, By by) {
        try {
            return element.findElement(by);
        } catch (NoSuchElementException e) {
            return null;
        } catch (Throwable e) {
            logError("Failed to find " + by, e);
            return null;
        }
    }


    /**
     * Finds the element for the `by`, clears the field and sends the given text
     *
     * @return the element or null if it is not found
     */
    public WebElement clearAndSendKeys(By by, String text) {
        WebElement field = findOptionalElement(by);
        if (field != null) {
            field.clear();
            field.sendKeys(text);
        }
        return field;
    }

    /**
     * Returns a form facade for submitting a form
     */
    public FormFacade form() {
        return new FormFacade(this);
    }

    public boolean until(ExpectedCondition<Boolean> condition) {
        return until(condition.toString(), defaultTimeoutInSeconds, condition);
    }

    public boolean until(String message, ExpectedCondition<Boolean> condition) {
        return until(message, defaultTimeoutInSeconds, condition);
    }

    public boolean until(String message, long timeoutInSeconds, ExpectedCondition<Boolean> condition) {
        return new WebDriverWait(getDriver(), timeoutInSeconds).withMessage(message).until(condition);
    }

    public boolean untilLinkClicked(final By by) {
        return untilLinkClicked(defaultTimeoutInSeconds, by);
    }


    public boolean untilSelectedByVisibleText(By by, String value) {
        return untilSelectedByVisibleText(defaultTimeoutInSeconds, by, value);
    }


    public boolean untilSelectedByVisibleText(long timeoutInSeconds, final By by, final String value) {
        String message = "select " + by + " with value: " + value;
        return new WebDriverWait(getDriver(), timeoutInSeconds).withMessage(message).until(new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver webDriver) {
                WebElement element = findOptionalElement(by);
                if (element != null && element.isEnabled()) {
                    Select select = new Select(element);
                    try {
                        select.selectByVisibleText(value);
                        logInfo("" + by + " select " + select + " selected value: " + value);
                        return true;
                    } catch (NoSuchElementException e) {
                        logWait("" + by + " select " + select + " does not yet have value: " + value);
                        return false;
                    }
                } else {
                    logWait("" + by + " not enabled");
                    return false;
                }
            }
        });
    }


    public boolean untilIsDisplayed(By firstBy, By secondBy) {
        return untilIsDisplayed(defaultTimeoutInSeconds, firstBy, secondBy);
    }

    public boolean untilIsDisplayed(long timeoutInSeconds, final By firstBy, final By secondBy) {
        String message = "" + firstBy + " then  " + secondBy + " is displayed";
        return until(message, timeoutInSeconds, new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver driver) {
                WebElement element = findOptionalElement(firstBy);
                if (element == null) {
                    logWait("" + firstBy + "");
                    return false;
                } else {
                    WebElement link = findOptionalElement(element, secondBy);
                    if (link != null && link.isDisplayed()) {
                        logInfo("" + firstBy + " then " + secondBy + " displayed");
                        return true;
                    } else {
                        logWait("" + firstBy + " then " + secondBy + " displayed");
                        return false;
                    }
                }
            }
        });
    }

    public boolean untilElementPredicate(final By by, final Function<WebElement, Boolean> elementPredicate) {
        return untilElementPredicate(defaultTimeoutInSeconds, by, elementPredicate);
    }

    public boolean untilElementPredicate(long timeoutInSeconds, final By by, final Function<WebElement, Boolean> elementPredicate) {
        String message = "" + by + " matches  " + elementPredicate;
        return until(message, timeoutInSeconds, new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver driver) {
                WebElement element = findOptionalElement(by);
                if (element == null) {
                    logWait("" + by + "");
                    return false;
                } else {
                    Boolean value = elementPredicate.apply(element);
                    if (value != null && value.booleanValue()) {
                        logInfo("" + by + " matches  " + elementPredicate + "");
                        return true;
                    } else {
                        logWait("" + by + " matches  " + elementPredicate + "");
                        return false;
                    }
                }
            }
        });
    }

    public boolean untilIsDisplayed(final By by) {
        return untilIsDisplayed(defaultTimeoutInSeconds, by);
    }

    public boolean untilIsDisplayed(long timeoutInSeconds, final By by) {
        return untilElementPredicate(timeoutInSeconds, by, new Function<WebElement, Boolean>() {
            @Override
            public String toString() {
                return "element.isDisplayed()";
            }

            @Override
            public Boolean apply(WebElement element) {
                return element.isDisplayed();
            }
        });
    }

    public boolean untilIsEnabled(final By by) {
        return untilIsEnabled(defaultTimeoutInSeconds, by);
    }

    public boolean untilIsEnabled(long timeoutInSeconds, final By by) {
        return untilElementPredicate(timeoutInSeconds, by, new Function<WebElement, Boolean>() {
            @Override
            public String toString() {
                return "element.isEnabled()";
            }

            @Override
            public Boolean apply(WebElement element) {
                return element.isEnabled();
            }
        });
    }

    public boolean untilLinkClicked(long timeoutInSeconds, final By by) {
        String message = "click link " + by;
        return until(message, timeoutInSeconds, new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver driver) {
                WebElement link = findOptionalElement(by);
                if (link != null) {
                    logInfo("Clicking link: " + by + "");
                    link.click();
                    logInfo("Clicked link: " + by + " now");
                    return true;
                } else {
                    logInfo("Not found link " + by + "");
                    return false;
                }
            }
        });
    }

    /**
     * Waits until one of the given elements is available
     */
    public void untilOneOf(final By... bys) {
        final List<By> byList = Arrays.asList(bys);
        String message = "One of these is available: " + byList;
        until(message, defaultTimeoutInSeconds, new ExpectedCondition<Boolean>() {

            @Override
            public Boolean apply(WebDriver driver) {
                for (By by : bys) {
                    WebElement element = findOptionalElement(by);
                    if (element != null && element.isDisplayed() && element.isEnabled()) {
                        logInfo("Found " + element + " for " + by + "");
                        return true;
                    }
                }
                logInfo("Still not found any of " + byList + "");
                return false;
            }
        });
    }

    public void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    public boolean currentUrlStartsWith(String expectedUrl) {
        String currentUrl = getDriver().getCurrentUrl();
        boolean answer = currentUrl != null && currentUrl.startsWith(expectedUrl);
        if (!answer) {
            logWarn("Current URL `" + currentUrl + "` does not start with `" + expectedUrl + "`");
        }
        return answer;
    }

    public void assertCurrentUrlStartsWith(String expectedUrl) {
        String currentUrl = getDriver().getCurrentUrl();
        boolean answer = currentUrl != null && currentUrl.startsWith(expectedUrl);
        if (!answer) {
            fail("Current URL `" + currentUrl + "` does not start with `" + expectedUrl + "`");
        }
    }

    /**
     * Lets wait until the link is visible then click it. If the click doesn't work lets retry a few times
     * just in case
     */
    public void untilLinkClickedLoop(By by, String expectedUrl) {
        for (int i = 0; i < 10; i++) {
            untilLinkClicked(by);
            sleep(Millis.seconds(10));

            if (currentUrlStartsWith(expectedUrl)) {
                break;
            } else {
                logWarn("lets try re-clicking link: " + by);
            }
        }
        assertCurrentUrlStartsWith(expectedUrl);
    }

    public long getDefaultTimeoutInSeconds() {
        return defaultTimeoutInSeconds;
    }

    public void setDefaultTimeoutInSeconds(long defaultTimeoutInSeconds) {
        this.defaultTimeoutInSeconds = defaultTimeoutInSeconds;
    }
}

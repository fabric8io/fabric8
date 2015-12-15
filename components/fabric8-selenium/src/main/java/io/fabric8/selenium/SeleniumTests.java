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
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.utils.Systems;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.opera.OperaDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.safari.SafariDriver;

/**
 * Helper methods for using Selenium tests with Kubernetes
 */
public class SeleniumTests {
    public static final String WAIT_AFTER_SELENIUM = "WAIT_AFTER_SELENIUM";
    public static final String FABRIC8_WEBDRIVER_NAME = "FABRIC8_WEBDRIVER_NAME";

    public static <T> T assertWebDriverForService(KubernetesClient client, String namespace, String serviceName, Function<WebDriverFacade, T> block) throws Exception {
        WebDriver driver = createWebDriver();
        try {
            WebDriverFacade facade = new WebDriverFacade(driver, client, namespace);
            facade.navigateToService(serviceName);

            T apply = block.apply(facade);

            String property = Systems.getEnvVarOrSystemProperty(WAIT_AFTER_SELENIUM);
            if (property != null) {
                long millis = 0;
                try {
                    millis = Long.parseLong(property);
                } catch (NumberFormatException e) {
                    logWarn("Env var / system property " + WAIT_AFTER_SELENIUM + " is not a long value: " + property + ". " + e, e);
                }
                if (millis > 0) {
                    logInfo("Sleeping for " + millis + " millis before tearning down the test case");
                    try {
                        Thread.sleep(millis);
                    } catch (InterruptedException e) {
                        // ignore
                    }
                }
            }
            return apply;
        } catch (Exception e) {
            logError("Failed with exception: ", e);
            throw e;
        } finally {
            driver.quit();
        }
    }

    public static WebDriver createWebDriver() {
        WebDriver answer = doCreateWebDriver();
        logInfo("Using WebDriver implementation: " + answer);
        return answer;
    }

    protected static WebDriver doCreateWebDriver() {
        String driverName = Systems.getEnvVarOrSystemProperty(FABRIC8_WEBDRIVER_NAME);
        if (driverName != null) {
            driverName = driverName.toLowerCase();
            if (driverName.equals("chrome")) {
                return new ChromeDriver();
            } else if (driverName.equals("edge")) {
                return new EdgeDriver();
            } else if (driverName.equals("firefox")) {
                return new FirefoxDriver();
            } else if (driverName.equals("htmlunit")) {
                return new HtmlUnitDriver();
            } else if (driverName.equals("internetexplorer") || driverName.equals("ie")) {
                return new InternetExplorerDriver();
            } else if (driverName.equals("opera")) {
                return new OperaDriver();
            } else if (driverName.equals("phantomjs")) {
                return new PhantomJSDriver();
/*
            } else if (driverName.equals("remote")) {
                return new RemoteWebDriver();
*/
            } else if (driverName.equals("safari")) {
                return new SafariDriver();
            } else if (driverName.equals("htmlunit")) {
                return new HtmlUnitDriver();
            }
        }
        return new ChromeDriver();
    }

    public static void logError(String message, Throwable e) {
        System.out.println("ERROR: " + message + e);
        e.printStackTrace();
        Throwable cause = e.getCause();
        if (cause != null && cause != e) {
            logError("Caused by: ", cause);
        }
    }

    public static void logWait(String message) {
        System.out.println("WAITING: " + message);
    }

    public static void logInput(String message) {
        System.out.println("INPUT: " + message);
    }

    public static void logClick(String message) {
        System.out.println("CLICK: " + message);
    }

    public static void logSubmit(String message) {
        System.out.println("SUBMIT: " + message);
    }

    public static void logInfo(String message) {
        System.out.println("INFO: " + message);
    }

    public static void logWarn(String message) {
        System.out.println("WARN: " + message);
    }

    public static void logWarn(String message, Throwable e) {
        System.out.println("WARN: " + message + e);
        e.printStackTrace();
        Throwable cause = e.getCause();
        if (cause != null && cause != e) {
            logWarn("Caused by: ", cause);
        }
    }

}

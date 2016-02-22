## Fabric8 Selenium 

This library provides a library to make it easier to create [Selenium WebDriver based](http://www.seleniumhq.org/projects/webdriver/) integration and system tests on Kubernetes using [fabric8-arquillian](https://github.com/fabric8io/fabric8/tree/master/components/fabric8-arquillian).

 
### Configuring the WebDriver implementation

[WebDriver based](http://www.seleniumhq.org/projects/webdriver/) supports a number of different drivers and browser. To configure this you can use the environment variable or system property called `FABRIC8_WEBDRIVER_NAME`.
 
Possible values of `FABRIC8_WEBDRIVER_NAME` are:
 
 * chrome
 * edge
 * firefox
 * htmlunit
 * internetexplorer / ie
 * opera
 * phantomjs
 * safari
 * htmlunit

To use Chrome you will need to [download and  install `chromedriver`](https://sites.google.com/a/chromium.org/chromedriver/downloads) and add it to your `$PATH` before running the tests.  

### Add it to your Maven pom.xml

To be able to use this library add this to your [Apache Maven](http://maven.apache.org/) based project add this into your pom.xml

            <dependency>
                <groupId>io.fabric8</groupId>
                <artifactId>fabric8-selenium</artifactId>
                <version>2.2.96</version>
                <scope>test</scope>
            </dependency>



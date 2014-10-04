package io.fabric8.process.test;

public class SystemPropertiesMockRestServiceDirectory implements MockRestServiceDirectory {

    @Override
    public void publish(String serviceSymbol, String serviceUrl) {
        System.setProperty(serviceUrlProperty(serviceSymbol), serviceUrl);
    }

    public static String serviceUrlProperty(String serviceSymbol) {
        return String.format("service.%s.url", serviceSymbol);
    }

}

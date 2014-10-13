package io.fabric8.process.spring.boot.rest.simple.client;

public class Header {

    private final String headerKey;

    private final String headerValue;

    public Header(String headerKey, String headerValue) {
        this.headerKey = headerKey;
        this.headerValue = headerValue;
    }

    public Header(String headerKey, Object headerValue) {
        this(headerKey, headerValue.toString());
    }

    public static Header header(String headerKey, String headerValue) {
        return new Header(headerKey, headerValue);
    }

    public static Header header(String headerKey, Object headerValue) {
        return new Header(headerKey, headerValue);
    }

    public String key() {
        return headerKey;
    }

    public String value() {
        return headerValue;
    }

}

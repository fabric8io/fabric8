/**
 *  Copyright 2005-2014 Red Hat, Inc.
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

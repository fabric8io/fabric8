/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.quickstarts.eip;

import org.apache.camel.language.NamespacePrefix;
import org.apache.camel.language.XPath;

/**
 * This class contains business logic that determines the region for a country. It is used by the Camel route in this example.
 */
public class RegionSupport {

    public static final String AMER = "AMER";
    public static final String APAC = "APAC";
    public static final String EMEA = "EMEA";

    /**
     * Get the region code that corresponds to the given country code.
     * 
     * This method can be used as a plain Java method. However, when it is used inside a Camel route, the @XPath annotation will
     * evaluate the XPath expression and use the result as the method parameter. In this case, it will fetch the country code
     * from the order XML message. So, the method will determine the region code for the country that is in the XML message.
     * 
     * @param country the country code
     * @return the region code
     */
    public String getRegion(@XPath(value = "/order:order/order:customer/order:country",
        namespaces = @NamespacePrefix(prefix = "order", uri = "http://fabric8.com/examples/order/v7")) String country) {
        if (country.equals("AU")) {
            return APAC;
        } else if (country.equals("US")) {
            return AMER;
        } else {
            return EMEA;
        }
    }
}

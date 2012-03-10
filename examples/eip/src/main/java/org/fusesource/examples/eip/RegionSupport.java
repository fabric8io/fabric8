/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.fusesource.examples.eip;

import org.apache.camel.language.NamespacePrefix;
import org.apache.camel.language.XPath;

/**
 * This class contains some business logic that will be used within our Camel routes:
 * - determine the region for a country
 */
public class RegionSupport {

    public static final String AMER = "AMER";
    public static final String APAC = "APAC";
    public static final String EMEA = "EMEA";

    /**
     * Get the region code that corresponds to the given country code.
     *
     * This method can be used as a plain Java method, but when it is used inside a Camel route, the @XPath annotation will kick in,
     * evaluating the XPath expression and using the result as the method parameter.  In this case, it will fetch the country code
     * from the order XML message, so the method will determine the region code for the country that is in the XML message.
     *
     * @param country the country code
     * @return the region code
     */
    public String getRegion(@XPath(value = "/order:order/order:customer/order:country",
                                   namespaces = @NamespacePrefix(prefix = "order", uri = "http://fusesource.com/examples/order/v7"))
                            String country) {
        if (country.equals("AU")) {
            return APAC;
        } else if (country.equals("US")) {
            return AMER;
        } else {
            return EMEA;
        }
    }
}

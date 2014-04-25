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
package io.fabric8.cxf;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PrefixAddressResolver implements ServerAddressResolver {
    private static final transient Log LOG = LogFactory.getLog(PrefixAddressResolver.class);
    private String prefixAddress;

    public void setPrefixAddress(String prefixAddress) {
        this.prefixAddress = prefixAddress;
    }

    public String getPrefixAddress() {
        return prefixAddress;
    }
    @Override
    public String getFullAddress(String address) {
        // Current CXF only supports these schemas
        if (!(address.startsWith("http://") || address.startsWith("jms://")
                || address.startsWith("camel://") || address.startsWith("nmr://"))) {
            // we need to update the address with the prefixAddress as the Service is published with relative path
            if (prefixAddress == null || prefixAddress.trim().length() == 0) {
                LOG.warn("Set the full address for the CXF service to " + address + " , as the prefixAddress is empty.");
                return address;
            }
            return prefixAddress + address;
        } else {
            return address;
        }
    }
}

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
package io.fabric8.cxf.env;

public class CxfContextResolver {

    /**
     * SPI interface to get servlet context of available CxfServlet instance
     */
    abstract static class CxfContextResolverSPI {
        abstract public String getCxfServletContext();
    }

    private CxfContextResolverSPI spi = null;

    public CxfContextResolver() {
        try {
            spi = new OsgiCxfContextResolverSPI();
        } catch (Exception ex) {
            // TODO: add resolver for non-OSGi (Tomcat, Wildfly) environments
            spi = new CxfContextResolverSPI() {
                @Override
                public String getCxfServletContext() {
                    return "";
                }
            };
        }
    }

    public String getCxfServletContext() {
        return spi.getCxfServletContext();
    }

}

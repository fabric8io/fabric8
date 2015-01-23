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
package org.jboss.fuse.rhaccess;

import org.apache.log4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;


/**
 * TODO integrate with OSGi config
 */
public class Config {

    private final static Logger log = Logger.getLogger(Config.class);

    //private final SystemSettings settings;

    public Config() {
//        SystemManagerLocal systemManager = LookupUtil.getSystemManager();
//        settings = systemManager.getSystemSettings(LookupUtil.getSubjectManager().getOverlord());
    }

    /**
     * return same value as defined in WEB-INF/support.html
     *
     * @return version of rh-access-plugin being sent to RHA
     */
    public String getUserAgent() {
        return "redhat-jboss-fuse";
    }

    public boolean isBrokered() {
        return true;
    }

    public String getURL() {
        return "https://api.access.redhat.com";
    }

    public String getProxyUser() {
        return null;
    }

    public String getProxyPassword() {
        return null;
    }

    public URL getProxyURL() {
        try {
            String url = null;
            if (url == null) {
                return null;
            }
            return new URL(url);
        } catch (MalformedURLException e) {
            log.error("Unable to parse PROXY_SERVER_HOST setting to URL", e);
            return null;
        }
    }

    public int getProxyPort() {
        String port = null;
        if (port == null) {
            port = "0";
        }
        return Integer.parseInt(port);
    }

    public int getSessionTimeout() {
        return 3000000;
    }

    public boolean isDevel() {
        return false;
    }
}

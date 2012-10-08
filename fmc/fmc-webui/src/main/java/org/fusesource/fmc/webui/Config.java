/*
 * Copyright 2012 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.fusesource.fmc.webui;


import org.apache.log4j.Logger;

/**
 * @author Stan Lewis
 */
public class Config {

    static private Config _instance = null;

    static private final Logger LOG = Logger.getLogger(Config.class);

    private String contentDirectory = null;

    public Config() {
        _instance = this;
        LOG.debug("FMC Dev config object created");
    }

    public void setContentDirectory(String directory) {
        LOG.debug("Setting content directory to " + directory);
        this.contentDirectory = directory;
    }

    public String getContentDirectory() {
        return contentDirectory;
    }

    public static Config getInstance() {
        return _instance;
    }
}

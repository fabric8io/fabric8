/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.patch.impl;

import org.fusesource.patch.BundleUpdate;

public class BundleUpdateImpl implements BundleUpdate {

    private final String symbolicName;
    private final String newVersion;
    private final String newLocation;
    private final String previousVersion;
    private final String previousLocation;

    public BundleUpdateImpl(String symbolicName, String newVersion, String newLocation, String previousVersion, String previousLocation) {
        this.symbolicName = symbolicName;
        this.newVersion = newVersion;
        this.newLocation = newLocation;
        this.previousVersion = previousVersion;
        this.previousLocation = previousLocation;
    }

    public String getSymbolicName() {
        return symbolicName;
    }

    public String getNewVersion() {
        return newVersion;
    }

    public String getNewLocation() {
        return newLocation;
    }

    public String getPreviousVersion() {
        return previousVersion;
    }

    public String getPreviousLocation() {
        return previousLocation;
    }

}

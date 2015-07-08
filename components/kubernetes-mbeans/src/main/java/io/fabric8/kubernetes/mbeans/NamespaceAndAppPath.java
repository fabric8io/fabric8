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
package io.fabric8.kubernetes.mbeans;

/**
 */
public class NamespaceAndAppPath {
    private final String namespace;
    private final String appPath;

    public NamespaceAndAppPath(String namespace, String appPath) {
        this.namespace = namespace;
        this.appPath = appPath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NamespaceAndAppPath that = (NamespaceAndAppPath) o;

        if (!appPath.equals(that.appPath)) return false;
        if (namespace != null ? !namespace.equals(that.namespace) : that.namespace != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = namespace != null ? namespace.hashCode() : 0;
        result = 31 * result + appPath.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "NamespaceAndAppPath{" +
                "namespace='" + namespace + '\'' +
                ", appPath='" + appPath + '\'' +
                '}';
    }

    public String getAppPath() {
        return appPath;
    }

    public String getNamespace() {
        return namespace;
    }
}

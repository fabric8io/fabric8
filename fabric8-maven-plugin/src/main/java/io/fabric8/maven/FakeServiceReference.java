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
package io.fabric8.maven;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

public class FakeServiceReference<S> implements ServiceReference<S> {

    private String clazz;
    private String filter;

    public FakeServiceReference(String clazz, String filter) {
        this.clazz = clazz;
        this.filter = filter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FakeServiceReference that = (FakeServiceReference) o;

        if (clazz != null ? !clazz.equals(that.clazz) : that.clazz != null) return false;
        if (filter != null ? !filter.equals(that.filter) : that.filter != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = clazz != null ? clazz.hashCode() : 0;
        result = 31 * result + (filter != null ? filter.hashCode() : 0);
        return result;
    }

    @Override
    public Object getProperty(String key) {
        return null;
    }

    @Override
    public String[] getPropertyKeys() {
        return new String[0];
    }

    @Override
    public Bundle getBundle() {
        return null;
    }

    @Override
    public Bundle[] getUsingBundles() {
        return new Bundle[0];
    }

    @Override
    public boolean isAssignableTo(Bundle bundle, String className) {
        return false;
    }

    @Override
    public int compareTo(Object reference) {
        return 0;
    }

}

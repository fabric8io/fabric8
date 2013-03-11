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
package org.fusesource.insight.metrics.model;

import javax.management.ObjectName;
import java.util.Map;

public class MBeanAttrResult {

    private final ObjectName objectName;
    private final Map<String, Object> attrs;

    public MBeanAttrResult(ObjectName objectName, Map<String, Object> attrs) {
        this.objectName = objectName;
        this.attrs = attrs;
    }

    public ObjectName getObjectName() {
        return objectName;
    }

    public Map<String, Object> getAttrs() {
        return attrs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MBeanAttrResult that = (MBeanAttrResult) o;

        if (attrs != null ? !attrs.equals(that.attrs) : that.attrs != null) return false;
        if (objectName != null ? !objectName.equals(that.objectName) : that.objectName != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = objectName != null ? objectName.hashCode() : 0;
        result = 31 * result + (attrs != null ? attrs.hashCode() : 0);
        return result;
    }
}

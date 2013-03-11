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

import java.util.List;

public class MBeanAttrs extends Request {

    protected final String obj;
    protected final List<String> attrs;

    public MBeanAttrs(String name, String obj, List<String> attrs) {
        super(name);
        this.obj = obj;
        this.attrs = attrs;
    }

    @Override
    public String getType() {
        return "attrs";
    }

    public String getObj() {
        return obj;
    }

    public List<String> getAttrs() {
        return attrs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MBeanAttrs mBeanAttrs = (MBeanAttrs) o;

        if (attrs != null ? !attrs.equals(mBeanAttrs.attrs) : mBeanAttrs.attrs != null) return false;
        if (name != null ? !name.equals(mBeanAttrs.name) : mBeanAttrs.name != null) return false;
        if (obj != null ? !obj.equals(mBeanAttrs.obj) : mBeanAttrs.obj != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (obj != null ? obj.hashCode() : 0);
        result = 31 * result + (attrs != null ? attrs.hashCode() : 0);
        return result;
    }
}

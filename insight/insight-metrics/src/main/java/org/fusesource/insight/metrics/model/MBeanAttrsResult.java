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

public class MBeanAttrsResult extends Result<MBeanAttrs> {

    private final List<MBeanAttrResult> results;

    public MBeanAttrsResult(MBeanAttrs request, List<MBeanAttrResult> results) {
        super(request);
        this.results = results;
    }

    @Override
    public MBeanAttrs getRequest() {
        return super.getRequest();
    }

    public List<MBeanAttrResult> getResults() {
        return results;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MBeanAttrsResult that = (MBeanAttrsResult) o;

        if (results != null ? !results.equals(that.results) : that.results != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return results != null ? results.hashCode() : 0;
    }
}

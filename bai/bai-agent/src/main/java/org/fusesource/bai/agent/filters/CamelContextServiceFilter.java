/*
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
package org.fusesource.bai.agent.filters;

import org.fusesource.bai.agent.CamelContextService;
import org.fusesource.common.util.Filter;

/**
 */
public class CamelContextServiceFilter implements Filter<CamelContextService> {
    private final Filter<String> bundleSymbolicNameFilter;
    private final Filter<String> camelContextIdFilter;

    public CamelContextServiceFilter(Filter<String> bundleSymbolicNameFilter, Filter<String> camelContextIdFilter) {
        this.bundleSymbolicNameFilter = bundleSymbolicNameFilter;
        this.camelContextIdFilter = camelContextIdFilter;
    }

    @Override
    public String toString() {
        return "CamelContextServiceFilter(bundleSymbolicName: " + bundleSymbolicNameFilter + ", camelContextId: " + camelContextIdFilter + ")";
    }

    @Override
    public boolean matches(CamelContextService camelContextService) {
        String bundleSymbolicName = camelContextService.getBundleSymbolicName();
        String camelContextId = camelContextService.getCamelContextId();
        return bundleSymbolicNameFilter.matches(bundleSymbolicName) && camelContextIdFilter.matches(camelContextId);
    }
}

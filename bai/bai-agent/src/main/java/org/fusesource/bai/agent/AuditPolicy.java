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
package org.fusesource.bai.agent;

import org.fusesource.bai.AuditEventNotifier;

/**
 * A policy to enable/disable and configure auditing levels for a given {@link CamelContextService}
 */
public interface AuditPolicy {

    /**
     * Returns true if auditing should be enabled for this camel context service
     */
    public boolean isAuditEnabled(CamelContextService service);

    /**
     * Creates a configured {@link AuditEventNotifier} for the given camel context service
     */
    public AuditEventNotifier createAuditNotifier(CamelContextService service);
}

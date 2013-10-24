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
package org.fusesource.gateway.fabric.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a routing rule
 */
public class RuleConfig {
    private static final transient Logger LOG = LoggerFactory.getLogger(RuleConfig.class);

    private PatternConfig from;
    private PatternConfig to;


    // Properties
    //-------------------------------------------------------------------------
    public PatternConfig getFrom() {
        return from;
    }

    public void setFrom(PatternConfig from) {
        this.from = from;
    }

    public PatternConfig getTo() {
        return to;
    }

    public void setTo(PatternConfig to) {
        this.to = to;
    }
}

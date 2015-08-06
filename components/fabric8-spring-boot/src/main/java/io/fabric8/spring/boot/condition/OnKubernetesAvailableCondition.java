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
package io.fabric8.spring.boot.condition;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.internal.Utils;
import io.fabric8.utils.Strings;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class OnKubernetesAvailableCondition extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String masterUrl = Utils.getSystemPropertyOrEnvVar(Config.KUBERNETES_MASTER_SYSTEM_PROPERTY);
        if (!Strings.isNullOrBlank(masterUrl)) {
           return ConditionOutcome.match();
        }
        return ConditionOutcome.noMatch("Url to kubernetes master, not found in environment variables or system properties.");
    }
}

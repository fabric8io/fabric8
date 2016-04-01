/**
 *  Copyright 2005-2016 Red Hat, Inc.
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

import io.fabric8.utils.Strings;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class OnInsideKubernetesCondition extends SpringBootCondition {

    private final String HOSTNAME = "HOSTNAME";
    private final String KUBERNETES_SERVICE_HOST = "KUBERNETES_SERVICE_HOST";
    private final String KUBERNETES_SERVICE_PORT = "KUBERNETES_SERVICE_PORT";

    private final String[] REQUIRED_ENV_VARIABLES = new String[]{HOSTNAME, KUBERNETES_SERVICE_HOST, KUBERNETES_SERVICE_PORT};

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        for (String variable : REQUIRED_ENV_VARIABLES) {
            if (Strings.isNullOrBlank(System.getenv().get(variable))) {
                return ConditionOutcome.noMatch("Environment variable " + variable + " not found.");
            }
        }
        return ConditionOutcome.match();
    }
}

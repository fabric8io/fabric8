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

package io.fabric8.bai.xml;


import io.fabric8.bai.config.PolicySet;

/**
 * Implementations of this interface are capable of reading policies from different media and formats,
 * and normalising them to the beans in the io.fabric8.bai.policy.model package.
 *
 * @author Raul Kripalani
 */
public interface PolicySlurper {

    public PolicySet slurp();

    public PolicySet refresh();

    public PolicySet getPolicies();

}

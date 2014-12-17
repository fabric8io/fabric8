/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.kubernetes.assertions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 */
public class Asserts {

    /**
     * Asserts that the code block throws an {@link AssertionError) and returns it
     */
    public static <T> AssertionError assertAssertionError(Block block) throws Exception {
        AssertionError answer = null;
        try {
            block.invoke();
        } catch (AssertionError e) {
            answer = e;
            System.out.println("Caught expected assertion failure: " + e);
        } catch (Exception e) {
            throw e;
        }
        assertThat(answer).isNotNull();
        System.out.println("Caught expected assertion failure: " + answer);
        return answer;
    }
}

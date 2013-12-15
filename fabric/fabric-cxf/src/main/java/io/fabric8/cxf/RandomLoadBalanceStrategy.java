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
package io.fabric8.cxf;

import java.util.Random;

public class RandomLoadBalanceStrategy extends FabricLoadBalanceStrategySupport {
    private Random random;

    public RandomLoadBalanceStrategy() {
        random = new Random();
    }

    public String getNextAlternateAddress() {
        if (alternateAddressList.size() > 0) {
            return alternateAddressList.get(random.nextInt(alternateAddressList.size()));
        } else {
            throw new IllegalArgumentException("The AlternateAddressList is empty, please fresh the list shortly.");
        }
    }
}

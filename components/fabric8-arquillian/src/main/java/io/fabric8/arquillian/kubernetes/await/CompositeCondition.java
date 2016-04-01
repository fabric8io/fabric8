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
package io.fabric8.arquillian.kubernetes.await;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

public class CompositeCondition implements Callable<Boolean> {

    private final List<Callable<Boolean>> callableList;

    public CompositeCondition(Collection<Callable<Boolean>> callables) {
        this.callableList = new ArrayList<>(callables);
    }

    public CompositeCondition(Callable<Boolean>... callables) {
        this.callableList = Arrays.asList(callables);
    }

    @Override
    public Boolean call() throws Exception {
        boolean result = true;
        for (int i = 0; i < callableList.size() && result; i++) {
            result = result && callableList.get(i).call();

        }
        return result;
    }
}

/**
 *  Copyright 2005-2014 Red Hat, Inc.
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
package org.apache.curator.framework.recipes.cache;

import java.util.Comparator;

public class OperationComparator implements Comparator<Operation> {

    @Override
    public int compare(Operation left, Operation right) {
        if (left instanceof TreeEventOperation && right instanceof TreeEventOperation) {
            return 0;
        } else if (left instanceof TreeEventOperation) {
            return 1;
        } else if (right instanceof TreeEventOperation) {
            return -1;
        } else {
            return 0;
        }
    }
}

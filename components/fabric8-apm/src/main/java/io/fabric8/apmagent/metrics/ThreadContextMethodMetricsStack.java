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
package io.fabric8.apmagent.metrics;

class ThreadContextMethodMetricsStack {

    private ThreadContextMethodMetrics[] stack;
    private int pointer;

    ThreadContextMethodMetricsStack() {
        stack = new ThreadContextMethodMetrics[2];
    }

    ThreadContextMethodMetrics push(ThreadContextMethodMetrics value) {
        if (pointer + 1 >= stack.length) {
            resizeStack(stack.length * 2);
        }
        stack[pointer++] = value;
        return value;
    }

    ThreadContextMethodMetrics pop() {
        final ThreadContextMethodMetrics result = stack[--pointer];
        stack[pointer] = null;
        return result;
    }

    private void resizeStack(int newCapacity) {
        ThreadContextMethodMetrics[] newStack = new ThreadContextMethodMetrics[newCapacity];
        System.arraycopy(stack, 0, newStack, 0, Math.min(pointer, newCapacity));
        stack = newStack;
    }

    public String toString() {
        StringBuilder result = new StringBuilder("[");
        for (int i = 0; i < pointer; i++) {
            if (i > 0) {
                result.append(", ");
            }
            result.append(stack[i].getName());
        }
        result.append(']');
        return result.toString();
    }
}

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.kubernetes.assertions;

import io.fabric8.kubernetes.api.model.Pod;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.fail;

/**
 *
 */
public class MultiHasPodSelectionAssert implements HasPodSelectionAssert {
    private List<HasPodSelectionAssert> asserters;

    class MultiPodSelectionAssert extends AbstractPodSelectionAssert {

        @Override
        public List<Pod> getPods() {
            List<Pod> rc = new ArrayList<>();
            for (HasPodSelectionAssert asserter : asserters) {
                for (Pod pod : asserter.pods().getPods()) {
                    rc.add(pod);
                }
            }
            return rc;
        }

        @Override
        public MultiPodSelectionAssert isPodReadyForPeriod(final long notReadyTimeoutMS, final long readyPeriodMS) {
            // Do it in parallel so that this does not take longer and long if we have lots of asserters
            final AtomicReference<Throwable> failure = new AtomicReference<>();
            ArrayList<Thread> threads = new ArrayList<>(asserters.size());
            for (HasPodSelectionAssert a : asserters) {
                final HasPodSelectionAssert asserter = a;
                Thread thread = new Thread("MultiPodSelectionAssert"){
                    @Override
                    public void run() {
                        try {
                            asserter.pods().isPodReadyForPeriod(notReadyTimeoutMS, readyPeriodMS);
                        } catch (Throwable e) {
                            failure.set(e);
                        }
                    }
                };
                thread.start();
                threads.add(thread);
            }
            for (Thread thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    fail("Interrupted: "+e);
                }
            }
            Throwable throwable = failure.get();
            if( throwable!=null ) {
                if( throwable instanceof Error )
                    throw (Error)throwable;
                if( throwable instanceof RuntimeException )
                    throw (RuntimeException)throwable;
                throw new RuntimeException(throwable);
            }
            return this;
        }
    }

    public MultiHasPodSelectionAssert(List<HasPodSelectionAssert> asserters) {
        this.asserters = asserters;
    }

    @Override
    public AbstractPodSelectionAssert pods() {
        return new MultiPodSelectionAssert();
    }

}

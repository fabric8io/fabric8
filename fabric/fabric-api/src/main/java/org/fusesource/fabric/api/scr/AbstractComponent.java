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
package org.fusesource.fabric.api.scr;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An abstract base class for validatable components.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 13-Sep-2013
 */
public abstract class AbstractComponent implements Validatable {

    private final AtomicBoolean active = new AtomicBoolean();

    public void activateComponent() {
        active.set(true);
    }

    public void deactivateComponent() {
        active.set(false);
    }

    @Override
    public boolean isValid() {
        return active.get();
    }

    @Override
    public void assertValid() {
        if (isValid() == false) {
            RuntimeException rte = new InvalidComponentException();
            rte.printStackTrace();
            throw rte;
        }
    }
}
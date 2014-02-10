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
package io.fabric8.boot.commands.support;

import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.Validatable;
import io.fabric8.api.scr.ValidationSupport;

import org.apache.felix.gogo.commands.basic.AbstractCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An abstract base class for validatable command components.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 05-Feb-2014
 */
@ThreadSafe
public abstract class AbstractCommandComponent extends AbstractCommand implements Validatable {

    private static final transient Logger LOG = LoggerFactory.getLogger(AbstractCommandComponent.class);

    /* This uses volatile to make sure that every thread sees the last written value
     */
    private ValidationSupport active = new ValidationSupport();

    public void activateComponent() {
        active.setValid();
        LOG.info("activateComponent: " + this);
    }

    public void deactivateComponent() {
        LOG.info("deactivateComponent: " + this);
        active.setInvalid();
    }

    @Override
    public boolean isValid() {
        return active.isValid();
    }

    @Override
    public void assertValid() {
        active.assertValid();
    }
}
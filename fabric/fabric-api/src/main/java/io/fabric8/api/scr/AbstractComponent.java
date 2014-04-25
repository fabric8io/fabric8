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
package io.fabric8.api.scr;

import io.fabric8.api.jcip.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An abstract base class for validatable components.
 *
 * @since 13-Sep-2013
 */
@ThreadSafe
public abstract class AbstractComponent implements Validatable {

    private static final transient Logger LOG = LoggerFactory.getLogger(AbstractComponent.class);

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

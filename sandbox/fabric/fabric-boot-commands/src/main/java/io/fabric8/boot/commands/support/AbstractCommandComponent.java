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
package io.fabric8.boot.commands.support;

import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.Validatable;
import io.fabric8.api.scr.ValidationSupport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.gogo.commands.basic.AbstractCommand;
import org.apache.karaf.shell.console.CompletableFunction;
import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.NullCompleter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An abstract base class for validatable command components.
 *
 * @since 05-Feb-2014
 */
@ThreadSafe
public abstract class AbstractCommandComponent extends AbstractCommand implements Validatable, CompletableFunction {

    private static final transient Logger LOG = LoggerFactory.getLogger(AbstractCommandComponent.class);

    private final ValidationSupport active = new ValidationSupport();
    private final List<Completer> completers = new ArrayList<Completer>();
    private final Map<String, Completer> optionalCompleters = new HashMap<String, Completer>();

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

    @Override
    public List<Completer> getCompleters() {
        synchronized (completers) {
            if (completers.isEmpty()) {
                return Arrays.<Completer>asList(new NullCompleter());
            }
            return Collections.unmodifiableList(completers);
        }
    }

    @Override
    public Map<String, Completer> getOptionalCompleters() {
        synchronized (optionalCompleters) {
            return Collections.unmodifiableMap(optionalCompleters);
        }
    }

    protected void bindCompleter(Completer completer) {
        synchronized (completers) {
            completers.add(completer);
        }
    }

    protected void unbindCompleter(Completer completer) {
        synchronized (completers) {
            completers.remove(completer);
        }
    }

    protected void bindOptionalCompleter(ParameterCompleter completer) {
        synchronized (optionalCompleters) {
            optionalCompleters.put(completer.getParameter(), completer);
        }
    }

    protected void unbindOptionalCompleter(ParameterCompleter completer) {
        synchronized (optionalCompleters) {
            optionalCompleters.remove(completer.getParameter());
        }
    }

    protected void bindOptionalCompleter(String key, Completer completer) {
        synchronized (optionalCompleters) {
            optionalCompleters.put(key, completer);
        }
    }

    protected void unbindOptionalCompleter(String key) {
        synchronized (optionalCompleters) {
            optionalCompleters.remove(key);
        }
    }
}

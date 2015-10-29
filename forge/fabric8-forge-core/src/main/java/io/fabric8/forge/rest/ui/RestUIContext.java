/**
 *  Copyright 2005-2015 Red Hat, Inc.
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
package io.fabric8.forge.rest.ui;

import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.ui.context.AbstractUIContext;
import org.jboss.forge.addon.ui.context.UISelection;
import org.jboss.forge.addon.ui.util.Selections;

import java.io.File;

public class RestUIContext extends AbstractUIContext {
    private final Resource<?> selection;
    private final RestUIProvider provider = new RestUIProvider();
    private String commitMessage;

    public RestUIContext() {
        this.selection = null;
    }

    public RestUIContext(Resource<?> selection) {
        super();
        this.selection = selection;
    }

    public File getInitialSelectionFile() {
        if (selection != null) {
            String fullyQualifiedName = selection.getFullyQualifiedName();
            if (fullyQualifiedName != null) {
                return new File(fullyQualifiedName);
            }
            String name = selection.getName();
            if (name != null) {
                return new File(name);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <SELECTIONTYPE> UISelection<SELECTIONTYPE> getInitialSelection() {
        return (UISelection<SELECTIONTYPE>) Selections.from(selection);
    }

    @Override
    public RestUIProvider getProvider() {
        return provider;
    }

    public String getCommitMessage() {
        return commitMessage;
    }

    public void setCommitMessage(String commitMessage) {
        this.commitMessage = commitMessage;
    }
}

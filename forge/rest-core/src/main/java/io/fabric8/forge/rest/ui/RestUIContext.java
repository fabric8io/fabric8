package io.fabric8.forge.rest.ui;

import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.ui.context.AbstractUIContext;
import org.jboss.forge.addon.ui.context.UISelection;
import org.jboss.forge.addon.ui.util.Selections;

import java.io.File;

public class RestUIContext extends AbstractUIContext {
    private final Resource<?> selection;
    private final RestUIProvider provider = new RestUIProvider();

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
            System.out.println("==== no fully qualified name!");
            String name = selection.getName();
            if (name != null) {
                return new File(name);
            }
            System.out.println("=========== Resource has no name! " + selection);
        } else {
            System.out.println("============ no selection!");
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

}

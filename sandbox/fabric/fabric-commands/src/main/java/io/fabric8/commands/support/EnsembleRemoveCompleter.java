package io.fabric8.commands.support;

import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.boot.commands.support.AbstractContainerCompleter;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.karaf.shell.console.Completer;

@Component(immediate = true)
@Service({ EnsembleRemoveCompleter.class, Completer.class })
public class EnsembleRemoveCompleter extends AbstractContainerCompleter {
    @Reference
    private FabricService fabricService;

    @Activate
    void activate() {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    @Override
    public FabricService getFabricService() {
        return fabricService;
    }

    public void setFabricService(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    @Override
    public boolean apply(Container container) {
        return container.isEnsembleServer();
    }
}

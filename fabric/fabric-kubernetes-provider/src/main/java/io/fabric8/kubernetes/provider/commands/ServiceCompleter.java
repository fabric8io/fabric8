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
package io.fabric8.kubernetes.provider.commands;

import io.fabric8.boot.commands.support.AbstractCompleterComponent;
import io.fabric8.kubernetes.api.Kubernetes;
import io.fabric8.kubernetes.api.model.PodListSchema;
import io.fabric8.kubernetes.api.model.PodSchema;
import io.fabric8.kubernetes.api.model.ServiceListSchema;
import io.fabric8.kubernetes.api.model.ServiceSchema;
import io.fabric8.kubernetes.provider.KubernetesService;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Component(immediate = true)
@Service({ServiceCompleter.class, Completer.class})
public final class ServiceCompleter extends AbstractCompleterComponent {
    private static final transient Logger LOG = LoggerFactory.getLogger(ServiceCompleter.class);

    @Reference
    protected KubernetesService kubernetesService;

    @Activate
    void activate() {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    @Override
    public String getParameter() {
        return "--service";
    }

    @Override
    public int complete(String buffer, int cursor, List<String> candidates) {
        StringsCompleter delegate = new StringsCompleter();
        try {
            Kubernetes kubernetes = kubernetesService.getKubernetes();
            if (kubernetes != null) {
                ServiceListSchema list = kubernetes.getServices();
                if (list != null) {
                    List<ServiceSchema> items = list.getItems();
                    if (items != null) {
                        for (ServiceSchema item : items) {
                            String id = item.getId();
                            delegate.getStrings().add(id);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            LOG.warn("Caught: " + ex, ex);
        }
        return delegate.complete(buffer, cursor, candidates);
    }

    public KubernetesService getKubernetesService() {
        return kubernetesService;
    }

    public void setKubernetesService(KubernetesService kubernetesService) {
        this.kubernetesService = kubernetesService;
    }
}

package io.fabric8.kubernetes.api;

import io.fabric8.kubernetes.api.model.Service;
import org.eclipse.jetty.websocket.client.WebSocketClient;

public class WatchServicesExample {

    public static void main(String... args) throws Exception {
        KubernetesClient kube = new KubernetesClient();
        System.out.println("Connecting to kubernetes on: " + kube.getAddress());
        WebSocketClient client = kube.watchServices("jimmi", null, new ExampleWatcher());
        Thread.sleep(10000l);
        client.stop();
    }

    static class ExampleWatcher extends AbstractWatcher<Service> {
        @Override
        public void eventReceived(Action action, Service object) {
            System.out.println(action + ": " + object);
        }
    }

}

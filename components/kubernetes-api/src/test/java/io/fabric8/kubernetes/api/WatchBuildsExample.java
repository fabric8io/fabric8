package io.fabric8.kubernetes.api;

import io.fabric8.openshift.api.model.Build;
import org.eclipse.jetty.websocket.client.WebSocketClient;

public class WatchBuildsExample {

    public static void main(String... args) throws Exception {
        KubernetesClient kube = new KubernetesClient();
        System.out.println("Connecting to kubernetes on: " + kube.getAddress());
        WebSocketClient client = kube.watchBuilds(new ExampleWatcher());
        Thread.sleep(10000l);
        client.stop();
    }

    static class ExampleWatcher extends AbstractWatcher<Build> {
        @Override
        public void eventReceived(Action action, Build object) {
            System.out.println(action + ": " + object);
        }
    }

}

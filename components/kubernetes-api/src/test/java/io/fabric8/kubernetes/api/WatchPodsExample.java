package io.fabric8.kubernetes.api;

import io.fabric8.kubernetes.api.model.Pod;
import org.eclipse.jetty.websocket.client.WebSocketClient;

public class WatchPodsExample {

    public static void main(String... args) throws Exception {
        KubernetesClient kube = new KubernetesClient();
        System.out.println("Connecting to kubernetes on: " + kube.getAddress());
        WebSocketClient client = kube.watchPods("default", null, new ExampleWatcher());
        Thread.sleep(10000l);
        client.stop();
    }

    static class ExampleWatcher extends AbstractWatcher<Pod> {
        @Override
        public void eventReceived(Watcher.Action action, Pod object) {
            System.out.println(action + ": " + object);
        }
    }

}

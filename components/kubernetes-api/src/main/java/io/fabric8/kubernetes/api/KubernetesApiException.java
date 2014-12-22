package io.fabric8.kubernetes.api;

public class KubernetesApiException extends Exception {

    public KubernetesApiException() {
    }

    public KubernetesApiException(String msg) {
        super(msg);
    }
}

package io.fabric8.kubernetes.api;

import io.fabric8.kubernetes.api.model.HasMetadata;

public interface Watcher<T extends HasMetadata> {

    enum Action {
        ADDED, MODIFIED, DELETED, ERROR
    }

    void eventReceived(Action action, T object);

}

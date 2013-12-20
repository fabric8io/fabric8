package io.fabric8.partition;

import java.net.URL;
import java.util.List;

public interface WorkItemRepository {

    /**
     * Start or resume the repository.
     * This method will activate the repository and will notify listeners of changes.
     */
    void start();

    /**
     * Suspends the repository.
     * When the repository is listeners will not get notified of changes.
     */
    void stop();

    /**
     * Completely shuts down the repository. Cannot be resumed.
     */
    void close();

    List<String> listWorkItemLocations();

    WorkItem readWorkItem(String location);

    void notifyListeners();

    void addListener(WorkItemListener workItemListener);

    void removeListener(WorkItemListener workItemListener);

}

package io.fabric8.partition;

public interface WorkItemRepositoryFactory {

    String ID_PREFIX = "io.fabric8.partition.repository.";
    String getType();
    WorkItemRepository build(String path);

}

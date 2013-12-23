package io.fabric8.partition;

import java.util.Map;

public interface TaskContext {

    String getId();
    Map<String, ?> getConfiguration();
}

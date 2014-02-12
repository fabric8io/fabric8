package io.fabric8.runtime.itests.support;

import io.fabric8.api.Container;

public interface ContainerCondition {
        Boolean checkConditionOnContainer(Container c);
}

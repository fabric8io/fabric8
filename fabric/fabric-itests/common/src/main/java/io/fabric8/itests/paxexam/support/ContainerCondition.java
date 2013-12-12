package io.fabric8.itests.paxexam.support;

import io.fabric8.api.Container;

public interface ContainerCondition {
        Boolean checkConditionOnContainer(Container c);
}

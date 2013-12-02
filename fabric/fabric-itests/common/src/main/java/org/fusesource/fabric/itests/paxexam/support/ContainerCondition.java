package org.fusesource.fabric.itests.paxexam.support;

import org.fusesource.fabric.api.Container;

public interface ContainerCondition {
        Boolean checkConditionOnContainer(Container c);
}

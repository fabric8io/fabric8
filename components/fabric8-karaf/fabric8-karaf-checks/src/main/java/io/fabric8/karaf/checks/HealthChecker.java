package io.fabric8.karaf.checks;

import java.util.List;

public interface HealthChecker {
    List<Check> getFailingHealthChecks();
}

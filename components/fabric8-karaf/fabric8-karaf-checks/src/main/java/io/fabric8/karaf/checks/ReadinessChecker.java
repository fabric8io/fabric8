package io.fabric8.karaf.checks;

import java.util.List;

public interface ReadinessChecker {
    List<Check> getFailingReadinessChecks();
}

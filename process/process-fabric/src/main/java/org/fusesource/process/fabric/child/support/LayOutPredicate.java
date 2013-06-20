package org.fusesource.process.fabric.child.support;

import com.google.common.base.Predicate;

public class LayOutPredicate implements Predicate<String> {

    private final String layOutPath;

    public LayOutPredicate(String layOutPath) {
        this.layOutPath = layOutPath;
    }

    @Override
    public boolean apply(java.lang.String input) {
        return input.startsWith(layOutPath);
    }
}

package org.fusesource.process.fabric.child.support;

import com.google.common.base.Predicate;

public class MvelPredicate implements Predicate<String> {

    public static final String MVEN_EXTENTION = ".mvel";

    @Override
    public boolean apply(String s) {
        return s.endsWith(MVEN_EXTENTION);
    }
}

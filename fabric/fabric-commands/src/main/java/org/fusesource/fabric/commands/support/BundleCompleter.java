package org.fusesource.fabric.commands.support;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;

public class BundleCompleter implements Completer {

    private Set<String> locations = new LinkedHashSet<String>();
    public static Callable<Set<String>> BUNDLE_LOCATION_SET;

    @Override
    public int complete(String buffer, int cursor, List<String> candidates) {
        StringsCompleter delegate = new StringsCompleter();
        delegate.getStrings().addAll(locations);
        int complete = delegate.complete(buffer, cursor, candidates);
        if (complete > 0) {
            return complete;
        } else {
            try {
                locations = BUNDLE_LOCATION_SET.call();
            } catch (Exception e) {
                //Ignore
            }
            delegate.getStrings().clear();
            delegate.getStrings().addAll(locations);
            return delegate.complete(buffer, cursor, candidates);
        }
    }
}

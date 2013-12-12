package io.fabric8.commands.support;

import java.util.LinkedList;
import java.util.List;
import org.apache.karaf.shell.console.Completer;

public class DelimitedListCompleter implements Completer {

    private Completer delegate;
    private String delimeter = ",";

    public DelimitedListCompleter(Completer delegate) {
        this.delegate = delegate;
    }

    @Override
    public int complete(String buffer, int cursor, List<String> candidates) {
        if (buffer == null || !buffer.contains(delimeter)) {
            return delegate.complete(buffer, cursor, candidates);
        } else {
            int pivot = buffer.lastIndexOf(",") + 1;
            int result =  delegate.complete(buffer.substring(pivot), cursor, candidates);
            List<String> updatedCandidates = new LinkedList<String>();
            for (String candidate:candidates) {
                candidate = buffer.substring(0, pivot) + candidate;
                updatedCandidates.add(candidate);
            }
            candidates.clear();
            for (String candidate:updatedCandidates) {
                candidates.add(candidate);
            }
            return result;
        }
    }

    public Completer getDelegate() {
        return delegate;
    }

    public void setDelegate(Completer delegate) {
        this.delegate = delegate;
    }

    public String getDelimeter() {
        return delimeter;
    }

    public void setDelimeter(String delimeter) {
        this.delimeter = delimeter;
    }
}

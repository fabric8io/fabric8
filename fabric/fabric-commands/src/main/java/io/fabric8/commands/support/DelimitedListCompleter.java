/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
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

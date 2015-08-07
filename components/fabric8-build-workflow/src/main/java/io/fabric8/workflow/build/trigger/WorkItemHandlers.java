/**
 *  Copyright 2005-2015 Red Hat, Inc.
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
package io.fabric8.workflow.build.trigger;

import io.fabric8.utils.Strings;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper methods for implementations of {@link org.kie.api.runtime.process.WorkItemHandler}
 */
public class WorkItemHandlers {
    private static final transient Logger LOG = LoggerFactory.getLogger(WorkItemHandlers.class);

    public static String getMandatoryParameter(WorkItem workItem, WorkItemManager manager, String parameterName) {
        String answer = (String) workItem.getParameter(parameterName);
        if (Strings.isNullOrBlank(answer)) {
            fail(workItem, manager, "Missing workflow parameter value '" + parameterName + "' but has parameters: " + workItem.getParameters());
        }
        return answer;
    }

    public static void fail(WorkItem workItem, WorkItemManager manager, String message, Throwable exception) {
        // lets unwrap the exception
        if (exception instanceof UndeclaredThrowableException ) {
            UndeclaredThrowableException undeclaredThrowableException = (UndeclaredThrowableException) exception;
            Throwable undeclaredThrowable = undeclaredThrowableException.getUndeclaredThrowable();
            if (undeclaredThrowable != null && !undeclaredThrowable.equals(exception)) {
                exception = undeclaredThrowable;
            }
        }

        String reason = message + ". Exception: " + exception;
        LOG.error("Failed work item " + workItem.getId() + ":" + workItem.getName() + ":  " + reason, exception);

        completeWithFailureReason(workItem, manager, reason);
    }

    public static void fail(WorkItem workItem, WorkItemManager manager, String reason) {
        LOG.error("Failed work item " + workItem.getId() + ":" + workItem.getName() + ":  " + reason);

        completeWithFailureReason(workItem, manager, reason);
    }

    protected static void completeWithFailureReason(WorkItem workItem, WorkItemManager manager, String reason) {
        Map<String, Object> result = new HashMap<>();
        result.put("Failed", reason);

        // complete with error or abort
        manager.completeWorkItem(workItem.getId(), result);
    }
}

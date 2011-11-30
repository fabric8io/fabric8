/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.agent.download;

import java.util.EventListener;

/**
 * Something interested in being notified when the completion
 * of an asynchronous download operation : {@link Future}.
 *
 * @author <a href="mailto:dev@mina.apache.org">Apache MINA SSHD Project</a>
 */
public interface FutureListener<T extends Future> extends EventListener {

    /**
     * Invoked when the operation associated with the {@link Future}
     * has been completed even if you add the listener after the completion.
     *
     * @param future The source {@link Future} which called this
     *               callback.
     */
    void operationComplete(T future);

}

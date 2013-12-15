/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package io.fabric8.jolokia.facade.dto;

/**
 * Author: lhein
 */
public class FabricServiceStatusDTO {

    public boolean managed;
    public Object clientConnectionError;
    public boolean clientValid;
    public boolean clientConnected;
    public boolean provisionComplete;

    @Override
    public String toString() {
        return String.format("FabricServiceStatus (clientConnected: %s, clientConnectionError: %s, clientValid: %s, managed: %s, provisionComplete: %s",
                    clientConnected,
                    clientConnectionError,
                    clientValid,
                    managed,
                    provisionComplete);
    }
}

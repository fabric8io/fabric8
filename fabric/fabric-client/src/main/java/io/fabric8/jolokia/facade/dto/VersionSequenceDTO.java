/*
 * ******************************************************************************
 *  * Copyright (c) 2013 Red Hat, Inc.
 *  * Distributed under license by Red Hat, Inc. All rights reserved.
 *  * This program is made available under the terms of the
 *  * Eclipse Public License v1.0 which accompanies this distribution,
 *  * and is available at http://www.eclipse.org/legal/epl-v10.html
 *  *
 *  * Contributors:
 *  *     Red Hat, Inc. - initial API and implementation
 *  *****************************************************************************
 */
package io.fabric8.jolokia.facade.dto;

public class VersionSequenceDTO {
    public String name;
    public int[] numbers;

    @Override
    public String toString() {
        return String.format("Version Sequence: %s ", name);
    }
}

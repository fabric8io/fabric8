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

import java.util.Collection;

/**
 * Author: lhein
 */
public class FabricRequirementsDTO {

    public Collection<ProfileRequirementsDTO> profileRequirements;

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("FabricRequirements: { ");
        if (profileRequirements != null) {
            for (ProfileRequirementsDTO dto : profileRequirements) {
                sb.append(dto);
            }
        }
        sb.append(" }");
        return sb.toString();
    }
}

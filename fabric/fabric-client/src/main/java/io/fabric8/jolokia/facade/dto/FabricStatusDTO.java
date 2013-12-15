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

import java.util.Map;

/**
 * Author: lhein
 */
public class FabricStatusDTO {
    public Map<String, ProfileRequirementsDTO> profileStatusMap;

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        if (profileStatusMap != null) {
            for (ProfileRequirementsDTO status : profileStatusMap.values()) {
                if (sb.length()>0) sb.append(", \n");
                sb.append(status);
            }
        }  else {
            sb.append("No ProfileStatus objects found...");
        }

        return sb.toString();
    }
}

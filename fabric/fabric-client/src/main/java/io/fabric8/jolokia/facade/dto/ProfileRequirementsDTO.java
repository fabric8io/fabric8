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
public class ProfileRequirementsDTO {
    public String profile;
    public Integer count;
    public Double health;
    public Integer minimumInstances;
    public Integer maximumInstances;
    public Collection<String> dependentProfiles;

    @Override
    public String toString() {
        String deps = "";
        if (dependentProfiles != null) {
            for (String dep : dependentProfiles) {
                if (deps.length()>0) deps += ", ";
                deps += dep;
            }
        }

        return String.format("ProfileRequirements: (profile: %s, minimumInstances: %d, maximumInstances: %d, count: %d, health: %s, dependentProfiles: %s)",
                profile,
                minimumInstances,
                maximumInstances,
                count,
                health,
                deps);
    }
}

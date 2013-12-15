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

import java.util.List;
import java.util.Map;

public class VersionDTO {
    public String id;
    public String name;
    public VersionSequenceDTO sequence;
    public VersionDTO derivedFrom;
    public Map<String, String> attributes;
    public List<String> profiles;
    public boolean defaultVersion;

    @Override
    public String toString() {
        return String.format("Version: { id: %s, name: %s, version: %s, derivedFrom: %s, profiles: %s }",
                id,
                name,
                (sequence != null ? sequence.name : ""),
                (derivedFrom != null ? derivedFrom.id : ""),
                profiles);
    }
}

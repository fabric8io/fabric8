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
package org.fusesource.fabric.jolokia.facade.dto;

import java.util.Map;
import java.util.Set;

public class VersionDTO {
    public String id;
    public VersionSequenceDTO sequence;
    public VersionDTO derivedFrom;
    public Map<String, String> attributes;
    public Set<ProfileDTO> profiles;
}

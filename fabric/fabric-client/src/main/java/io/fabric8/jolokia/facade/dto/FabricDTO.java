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
public class FabricDTO {
    public String MavenRepoURI;
    public String DefaultJvmOptions;
    public String MavenRepoUploadURI;
    public String ZookeeperUrl;
    public String CurrentContainerName;
    public String DefaultRepo;
    public FabricServiceStatusDTO FabricServiceStatus;

    @Override
    public String toString() {
        return String.format("Fabric: {\n" +
                "CurrentContainerName: %s\n" +
                "%s\n" +
                "Default JVM Options: %s\n" +
                "Default Repo: %s\n" +
                "Maven Repo URI: %s\n" +
                "Maven Repo Upload URI: %s\n" +
                "Zookeeper URL: %s}",
                CurrentContainerName,
                FabricServiceStatus,
                DefaultJvmOptions,
                DefaultRepo,
                MavenRepoURI,
                MavenRepoUploadURI,
                ZookeeperUrl);
    }
}

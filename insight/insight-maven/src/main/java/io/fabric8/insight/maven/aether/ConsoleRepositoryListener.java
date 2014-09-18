/**
 *  Copyright 2005-2014 Red Hat, Inc.
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
package io.fabric8.insight.maven.aether;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.RepositoryEvent;

public class ConsoleRepositoryListener extends AbstractRepositoryListener {

    private static final Logger LOG = LoggerFactory.getLogger(ConsoleRepositoryListener.class);

    @Override
    public void artifactDeployed(RepositoryEvent event) {
        LOG.debug("Deployed " + event.getArtifact() + " to " + event.getRepository());
    }

    @Override
    public void artifactDeploying(RepositoryEvent event) {
        LOG.debug("Deploying " + event.getArtifact() + " to " + event.getRepository());
    }

    @Override
    public void artifactDescriptorInvalid(RepositoryEvent event) {
        LOG.warn("Invalid artifact descriptor for " + event.getArtifact() +
            ": " + event.getException().getMessage());
    }

    @Override
    public void artifactDescriptorMissing(RepositoryEvent event) {
        LOG.warn("Missing artifact descriptor for " + event.getArtifact());
    }

    @Override
    public void artifactInstalled(RepositoryEvent event) {
        LOG.debug("Installed " + event.getArtifact() + " to " + event.getFile());
    }

    @Override
    public void artifactInstalling(RepositoryEvent event) {
        LOG.debug("Installing " + event.getArtifact() + " to " + event.getFile());
    }

    @Override
    public void artifactResolved(RepositoryEvent event) {
        LOG.debug("Resolved " + event.getArtifact() + " from " + event.getRepository());
    }

    @Override
    public void artifactResolving(RepositoryEvent event) {
        LOG.debug("Resolving " + event.getArtifact() + " from " + event.getRepository());
    }

    @Override
    public void metadataDeployed(RepositoryEvent event) {
        LOG.debug("Deployed " + event.getMetadata() + " to " + event.getRepository());
    }

    @Override
    public void metadataDeploying(RepositoryEvent event) {
        LOG.debug("Deploying " + event.getMetadata() + " to " + event.getRepository());
    }

    @Override
    public void metadataInstalled(RepositoryEvent event) {
        LOG.debug("Installed " + event.getMetadata() + " to " + event.getFile());
    }

    @Override
    public void metadataInstalling(RepositoryEvent event) {
        LOG.debug("Installing " + event.getMetadata() + " to " + event.getFile());
    }

    @Override
    public void metadataInvalid(RepositoryEvent event) {
        LOG.warn("Invalid metadata " + event.getMetadata());
    }

    @Override
    public void metadataResolved(RepositoryEvent event) {
        LOG.debug("Resolved " + event.getMetadata() + " from " + event.getRepository());
    }

    @Override
    public void metadataResolving(RepositoryEvent event) {
        LOG.debug("Resolving " + event.getMetadata() + " from " + event.getRepository());
    }

}

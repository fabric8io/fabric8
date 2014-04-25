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
package io.fabric8.insight.maven.aether

import org.sonatype.aether.{RepositoryEvent, AbstractRepositoryListener}
import org.slf4j.LoggerFactory

class ConsoleRepositoryListener extends AbstractRepositoryListener {

  val LOG = LoggerFactory.getLogger(classOf[ConsoleRepositoryListener])
  
  override def artifactDeployed(event: RepositoryEvent) =
    LOG.debug("Deployed " + event.getArtifact + " to " + event.getRepository)

  override def artifactDeploying(event: RepositoryEvent) =
    LOG.debug("Deploying " + event.getArtifact + " to " + event.getRepository)

  override def artifactInstalled(event: RepositoryEvent) =
    LOG.debug("Installed " + event.getArtifact + " to " + event.getFile)

  override def artifactInstalling(event: RepositoryEvent) =
    LOG.debug("Installing " + event.getArtifact + " to " + event.getFile)

  override def artifactResolved(event: RepositoryEvent) =
    LOG.debug("Resolved " + event.getArtifact + " from " + event.getRepository)

  override def artifactResolving(event: RepositoryEvent) =
    LOG.debug("Resolving " + event.getArtifact + " from " + event.getRepository)


  override def artifactDescriptorMissing(event: RepositoryEvent) =
    LOG.warn("Missing artifact descriptor for " + event.getArtifact)

  override def artifactDescriptorInvalid(event: RepositoryEvent) =
    LOG.warn("Invalid artifact descriptor for " + event.getArtifact +
            ": " + event.getException().getMessage)


  override def metadataDeployed(event: RepositoryEvent) =
    LOG.debug("Deployed " + event.getMetadata + " to " + event.getRepository)

  override def metadataDeploying(event: RepositoryEvent) =
    LOG.debug("Deploying " + event.getMetadata + " to " + event.getRepository)

  override def metadataInstalled(event: RepositoryEvent) =
    LOG.debug("Installed " + event.getMetadata + " to " + event.getFile)

  override def metadataInstalling(event: RepositoryEvent) =
    LOG.debug("Installing " + event.getMetadata + " to " + event.getFile)

  override def metadataResolved(event: RepositoryEvent) =
    LOG.debug("Resolved " + event.getMetadata + " from " + event.getRepository)

  override def metadataResolving(event: RepositoryEvent) =
    LOG.debug("Resolving " + event.getMetadata + " from " + event.getRepository)


  override def metadataInvalid(event: RepositoryEvent) =
    LOG.warn("Invalid metadata " + event.getMetadata)

}
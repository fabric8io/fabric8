package org.fusesource.insight.maven.aether

import org.sonatype.aether.{RepositoryEvent, AbstractRepositoryListener}
import org.fusesource.scalate.util.Logging

class ConsoleRepositoryListener extends AbstractRepositoryListener with Logging {

  override def artifactDeployed(event: RepositoryEvent) =
    debug("Deployed " + event.getArtifact + " to " + event.getRepository)

  override def artifactDeploying(event: RepositoryEvent) =
    debug("Deploying " + event.getArtifact + " to " + event.getRepository)

  override def artifactInstalled(event: RepositoryEvent) =
    debug("Installed " + event.getArtifact + " to " + event.getFile)

  override def artifactInstalling(event: RepositoryEvent) =
    debug("Installing " + event.getArtifact + " to " + event.getFile)

  override def artifactResolved(event: RepositoryEvent) =
    debug("Resolved " + event.getArtifact + " from " + event.getRepository)

  override def artifactResolving(event: RepositoryEvent) =
    debug("Resolving " + event.getArtifact + " from " + event.getRepository)


  override def artifactDescriptorMissing(event: RepositoryEvent) =
    warn("Missing artifact descriptor for " + event.getArtifact)

  override def artifactDescriptorInvalid(event: RepositoryEvent) =
    warn("Invalid artifact descriptor for " + event.getArtifact +
            ": " + event.getException().getMessage)


  override def metadataDeployed(event: RepositoryEvent) =
    debug("Deployed " + event.getMetadata + " to " + event.getRepository)

  override def metadataDeploying(event: RepositoryEvent) =
    debug("Deploying " + event.getMetadata + " to " + event.getRepository)

  override def metadataInstalled(event: RepositoryEvent) =
    debug("Installed " + event.getMetadata + " to " + event.getFile)

  override def metadataInstalling(event: RepositoryEvent) =
    debug("Installing " + event.getMetadata + " to " + event.getFile)

  override def metadataResolved(event: RepositoryEvent) =
    debug("Resolved " + event.getMetadata + " from " + event.getRepository)

  override def metadataResolving(event: RepositoryEvent) =
    debug("Resolving " + event.getMetadata + " from " + event.getRepository)


  override def metadataInvalid(event: RepositoryEvent) =
    warn("Invalid metadata " + event.getMetadata)

}
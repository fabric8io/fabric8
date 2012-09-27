package org.fusesource.insight.maven.resources

import javax.ws.rs._
import org.fusesource.insight.maven.aether.Aether


@Path("/projects")
class ProjectsResource {

  val aether = new Aether()

  @Path("project/{projectId: [^/]+}/{artifactId: [^/]+}/{extension: [^/]+}/{classifier: [^/]*}/{versionId: [^/]+}")
  def project(@PathParam("projectId") projectId: String,
           @PathParam("artifactId") artifactId: String,
           @PathParam("extension") extension: String,
           @PathParam("classifier") classifier: String,
           @PathParam("versionId") versionId: String) = {
    new ProjectResource(this, projectId, artifactId, extension, classifier, versionId)
  }

  @Path("compare/{projectId: [^/]+}/{artifactId: [^/]+}/{extension: [^/]+}/{classifier: [^/]*}/{version1: [^/]+}/{version2: [^/]+}")
  def compare(@PathParam("projectId") projectId: String,
           @PathParam("artifactId") artifactId: String,
           @PathParam("version1") version1: String,
           @PathParam("version2") version2: String,
           @PathParam("extension") extension: String,
           @PathParam("classifier") classifier: String) = {
    new CompareProjectResource(this, projectId, artifactId, version1, version2, extension, classifier)
  }

}
package org.fusesource.insight.maven.resources

import javax.ws.rs._
import collection.JavaConversions._
import collection.immutable.TreeMap

import org.fusesource.scalate.util.Logging
import org.sonatype.aether.graph.DependencyNode

class ProjectResource(parent: ProjectsResource, val groupId: String, val artifactId: String, val extension: String, val classifier: String, val version: String) extends Logging {

  def aether = parent.aether

  lazy val result = aether.resolve(groupId, artifactId, version, extension, classifier)

  lazy val products = LegalCsvReport.fuseProducts.filterNot(_ == groupId)

  def legalReports: Iterable[LegalReport] =
     LegalCsvReport.toLegalReports(result.root, products)

  @Path("csv")
  def csv = new ProjectCSV(this)
}


class ProjectCSV(parent: ProjectResource) {

  @GET
  @Produces(Array("text/csv", "text/comma-separated-values"))
  def csv: String = {
    LegalCsvReport.legalCsv(parent.legalReports)
  }

}


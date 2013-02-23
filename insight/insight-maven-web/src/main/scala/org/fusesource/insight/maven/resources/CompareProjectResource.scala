package org.fusesource.insight.maven.resources

import collection.JavaConversions._
import javax.ws.rs.core.Response
import javax.ws.rs._
import org.fusesource.scalate.util.Logging
import com.sun.jersey.api.view.{Viewable, ImplicitProduces}
import java.io.StringWriter
import org.fusesource.insight.maven.aether.CompareDependencyNode
import org.fusesource.insight.maven.util.CsvWriter

object CompareDependencyNodes {
  val all = (c: CompareDependencyNode) => true

  val addOrUpdated = (c: CompareDependencyNode) => c.change.isAddOrUpdate
}


class CompareProjectResource(parent: ProjectsResource, val groupId: String, val artifactId: String, val version1: String, val version2: String, val extension: String, val classifier: String) extends Logging {

  def aether = parent.aether

  lazy val result = aether.compare(groupId, artifactId, version1, version2, extension, classifier)

  lazy val products = LegalCsvReport.fuseProducts.filterNot(_ == groupId)

  override def toString = "CompareProjectResource(" + groupId + ":" + artifactId + ":" + version1 + "->" + version2 + ")"

  @Path("csv")
  def csv = new CompareProjectCSV(this, CompareDependencyNodes.all)

  @Path("newCsv")
  def newCsv = new CompareProjectCSV(this, CompareDependencyNodes.addOrUpdated)

  @Path("legal.csv")
  def legalCsv = new CompareProjectLegalCsv(this)

  def legalReports: Iterable[LegalReport] =
     LegalCsvReport.toLegalCompareReports(result.root, products)
}


class CompareProjectLegalCsv(parent: CompareProjectResource) {

  @GET
  @Produces(Array("text/csv", "text/comma-separated-values"))
  def csv: String = {
    LegalCsvReport.legalCsv(parent.legalReports)
  }
}


class CompareProjectCSV(parent: CompareProjectResource, filter: CompareDependencyNode => Boolean) {
  @GET
  @Produces(Array("text/csv","text/comma-separated-values"))
  def csv: String = {
    val buffer = new StringWriter()
    val out = new CsvWriter(buffer)
    out.println("GroupID", "ArtifactID", "NewVersion", "OldVersion")

    def toCsv(node: CompareDependencyNode, indent: Int = 0) {
      out.println(("  " * indent) + node.groupId, node.artifactId, node.version2.getOrElse(""), node.version1.getOrElse(""))

      val childIndent = indent + 1
      for (c <- node.children) {
        toCsv(c, childIndent)
      }
    }
    toCsv(parent.result.root)
    buffer.toString
  }

}
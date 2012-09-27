package org.fusesource.insight.maven.resources

import collection.immutable.TreeMap
import collection.JavaConversions._

import java.io.StringWriter

import org.fusesource.insight.maven.util.CsvWriter

import org.sonatype.aether.graph.DependencyNode
import org.fusesource.insight.maven.aether.CompareDependencyNode

object LegalCsvReport {

  val vendors = List(
    Vendor("Apache Software Foundation", "Apache 2.0", "commons-", "org.apache"),
    Vendor("AOP Alliance", "Apache 2.0", "aopalliance", "org.aopalliance"),
    Vendor("VMWare SpringSource", "Apache 2.0", "org.springframework"),
    Vendor("EPFL", "Apache 2.0", "org.scala-lang"),
    Vendor("Google", "Apache 2.0", "com.google.gwt"),
    Vendor("QOS.ch", "MIT", "ch.qos", "org.slf4j"),
    Vendor("Oracle", "CDDL", "com.sun.jersey", "com.sun.xml", "com.sun.tools", "com.sun.msv", "com.sun.mail"),
    Vendor("Codehaus", "Apache 2.0", "org.codehaus.jackson"),
    Vendor("INRIA, France Telecom", "BSD", "asm")
  )

  val fuseProducts = List[String]("org.apache.activemq", "org.apache.camel", "org.apache.cxf", "org.apache.karaf", "org.apache.servicemix")

  val ignoreGroupIds = List[String]("org.apache", "org.fusesource", "com.fusesource", "commons-", "log4j", "org.eclipse", "org.osgi")


  def legalCsv(seq: Traversable[LegalReport]): String = {
    val buffer = new StringWriter()
    legalCsv(seq, new CsvWriter(buffer))
    buffer.toString
  }

  def legalCsv(seq: Traversable[LegalReport], out: CsvWriter): Unit = {
    out.println("Third Party Product", "Vendor", "Changes from Previous Version?", "License Agreement",
      "License Type (by Legal)", "License Term (by Legal)",
      "Open Source? (Yes or No)", "Modified? (Yes or No)", "Embedded? (Yes or No)", "Source or Binary?",
      "Distribution Rights (by Legal)", "Copyright  and TM Notice (by Legal)",
      "EULA Requirements (by Legal)", "Assignment (by Legal)",
      "Notes	Purpose/Nature of Component	Royalty Payments Owed? (Yes or No)",
      "Royalty Details and Product Code List")

    for (r <- seq) {
      out.println(r.product, r.vendor, r.change, r.license, "", "", "Yes", "No", "Yes", "Binary", "", "", "", r.notes, "Library", "No", "")
    }
  }


  def toLegalReports(node: DependencyNode, productGroups: List[String]): Iterable[LegalReport] = {
    val map = legalDependencies(node, productGroups)
    toLegalReports(map)
  }

  def toLegalReports(depMap: Map[String, List[DependencyNode]]): Iterable[LegalReport] = {
    depMap.map{
      case (g, v) =>
        val artifacts = v.map{
            n =>
            val a = n.getDependency.getArtifact
            a.getArtifactId + "-" + a.getVersion + "." + a.getExtension
        }.mkString(" ")

        val product = g + " files: " + artifacts

        // TODO we should attempt to load the Project for the pom.xml for each of the
        // available artifacts and check the licenses and vendor..
        var vendor = ""
        var license = ""
        vendors.find(_.matches(g)) match {
          case Some(v) =>
            vendor = v.vendor
            license = v.license
          case _ =>
        }

        val change = "Added"
        val notes = ""
        LegalReport(product, vendor, license, change, notes)
    }
  }

  /**
   * Returns the legal dependencies ignoring our products and filtering out apache code
   */
  def legalDependencies(node: DependencyNode, productGroups: List[String], others: Map[String, List[DependencyNode]] = new TreeMap[String, List[DependencyNode]]()): Map[String, List[DependencyNode]] = {
    val d = node.getDependency
    val a = d.getArtifact
    val groupId = a.getGroupId
    var map = others

    if (d.isOptional) {
      println("Ignoring optional dependency: " + a)
    } else if (productGroups.exists(p => groupId.startsWith(p))) {
      println("Ignoring " + a + " as its a product")
    } else {
      if (ignoreGroupIds.exists(p => groupId.startsWith(p))) {
        println("Ignoring " + a + " as its an apache distro")
      } else {
        val list = map.getOrElse(groupId, List())
        val newList = list ++ List(node)
        map += (groupId -> newList)
      }

      for (c <- node.getChildren) {
        map = legalDependencies(c, productGroups, map)
      }
    }
    map
  }


  def toLegalCompareReports(node: CompareDependencyNode, productGroups: List[String]): Iterable[LegalReport] = {
    val map = legalCompareDependencies(node, productGroups)
    toLegalCompareReports(map)
  }

  def toLegalCompareReports(depMap: Map[String, List[CompareDependencyNode]]): Iterable[LegalReport] = {
    depMap.map{
      case (g, v) =>
        var notes = ""
        var change = "Added"

        val artifacts = v.map{
            n =>
            if (n.change.isUpdate) {
              change = "Updated"
            }
            val version1 = n.version1.getOrElse("??")
            val version2 = n.version2.getOrElse("??")
            notes +=  n.artifactId + "." + n.extension + "(" + version1 + " => " + version2 + ") "
            n.artifactId + "-" + version2 + "." + n.extension
        }.mkString(" ")

        val product = g + " files: " + artifacts

        var vendor = ""
        var license = ""
        vendors.find(_.matches(g)) match {
          case Some(v) =>
            vendor = v.vendor
            license = v.license
          case _ =>
        }
        LegalReport(product, vendor, license, change, notes)
    }
  }

  /**
   * Returns the legal dependencies ignoring our products and filtering out apache code
   */
  def legalCompareDependencies(node: CompareDependencyNode, productGroups: List[String], others: Map[String, List[CompareDependencyNode]] = new TreeMap[String, List[CompareDependencyNode]]()): Map[String, List[CompareDependencyNode]] = {
    val groupId = node.groupId
    var map = others

    lazy val description = node.groupId + ":" + node.artifactId

    if (node.isOptional) {
      println("Ignoring optional dependency: " + description)
    } else if (!node.change.isAddOrUpdate) {
      println("Ignoring non add/update change: " + description + " " + node.change)
    } else if (productGroups.exists(p => groupId.startsWith(p))) {
      println("Ignoring " + description + " as its a product")
    } else {
      if (ignoreGroupIds.exists(p => groupId.startsWith(p))) {
        println("Ignoring " + description + " as its an apache distro")
      } else {
        val list = map.getOrElse(groupId, List())
        val newList = list ++ List(node)
        map += (groupId -> newList)
      }

      for (c <- node.children) {
        map = legalCompareDependencies(c, productGroups, map)
      }
    }
    map
  }


}


case class LegalReport(product: String, vendor: String, license: String, change: String, notes: String)

case class Vendor(vendor: String, license: String, groupIdPrefixes: String*) {
  def matches(aGroupId: String) = groupIdPrefixes.exists(p => aGroupId.startsWith(p))
}










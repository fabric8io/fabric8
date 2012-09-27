package org.fusesource.insight.maven

import aether.{AetherPomResult, AetherJarOrPom, AetherResult}
import java.io.File
import org.sonatype.aether.graph.DependencyNode
//import collection.mutable.HashSet
import org.sonatype.aether.artifact.Artifact
import collection.JavaConversions._
import java.util.{HashSet, TreeMap}

object ArtifactHelper {
  def idWithVersion(a: Artifact): String = a.getGroupId + ":" + a.getArtifactId + ":" + a.getVersion + ":" + a.getExtension + ":" + a.getClassifier

  def idLessVersion(a: Artifact): String = a.getGroupId + ":" + a.getArtifactId + ":" + a.getExtension + ":" + a.getClassifier

  def getOrElseUpdate[K, V](map: TreeMap[K, V], key: K, value: => V): V = {
    var answer = map.get(key)
    if (answer == null) {
      answer = value
      map.put(key, value)
    }
    answer
  }
}

import ArtifactHelper._

class ProjectResolveTest extends LocalBuildTestSupport {

  test("aether") {
    //val postfix = "fuse/pom.xml"
    val postfix = "cade/pom.xml"
    // lets find the fuse project dir
    var file = new File("../" + postfix)
    if (!file.exists()) {
      file = new File("../../" + postfix)
    }
    assert(file.exists())
    val result = aether.resolveLocalProject(file)

    val idNoVersionMap = new TreeMap[String, ArtifactVersions]()

    def addDependencies(root: DependencyNode): Unit = {
      addChildDependencies(root, root)
    }

    def addChildDependencies(node: DependencyNode, owner: DependencyNode): Unit = {
      val artifact = node.getDependency.getArtifact
      val idNoVersion = idLessVersion(artifact)
      val version = artifact.getVersion

      var artifactVersions = idNoVersionMap.get(idNoVersion)
      if (artifactVersions == null) {
        artifactVersions = new ArtifactVersions(artifact)
        idNoVersionMap.put(idNoVersion, artifactVersions)
      }
      //val artifactVersions = getOrElseUpdate(idNoVersionMap, idNoVersion, new ArtifactVersions(artifact))
      val versionMap = artifactVersions.versionsToUseSet
      var useSet = versionMap.get(version)
      if (useSet == null) {
         useSet = new HashSet[DependencyNode]()
          versionMap.put(version, useSet)
      }
      //val useSet = getOrElseUpdate(versionMap, version, new HashSet[DependencyNode]())
      useSet.add(owner)
      versionMap.put(version, useSet)

      for (child <- node.getChildren) {
        addChildDependencies(child, owner)
      }
    }

    def addResultDependencies(r: AetherJarOrPom): Unit = {
      r match {
        case ap: AetherPomResult =>
          addDependencies(ap.root)
          for (m <- ap.modules) {
            addResultDependencies(m)
          }

        case ar: AetherResult =>
          addDependencies(ar.root)
          ar.root
      }
    }

    addResultDependencies(result)

    for (av <- idNoVersionMap.values()) {
      val versions = av.versionsToUseSet
      if (versions.size() > 1) {
        println("ERROR: Duplciate versions for " + av.artifact + " uses: " + versions)
      }
    }

    for (av <- idNoVersionMap.values()) {
      val k = av.artifact
      val versions = av.versionsToUseSet.keys.toList
      if (versions.isEmpty) {
        println("Warning no versions for " + k + " in " + av.versionsToUseSet)
      } else {
        val version = versions.last
        println("<dependency>")
        println("  <groupId>" + k.getGroupId + "</groupId>")
        println("  <artifactId>" + k.getArtifactId + "</artifactId>")
        println("  <version>" + version + "</version>")

        val classifier = k.getClassifier
        if (classifier != null && classifier.size > 0) {
          println("  <classifier>" + classifier + "</classifier>")
        }
        val extension = k.getExtension
        if (extension != null && extension.size > 0 && extension != "jar") {
          println("  <type>" + extension + "</type>")
        }
        println("</dependency>")
      }
    }
  }
}

class ArtifactVersions(val artifact: Artifact) {
  val versionsToUseSet = new TreeMap[String, HashSet[DependencyNode]]()

  //println("Creating new " + this)

  override def toString = "ArtifactVersions(" + idLessVersion(artifact) + " versions: " + versionsToUseSet + ")"
}
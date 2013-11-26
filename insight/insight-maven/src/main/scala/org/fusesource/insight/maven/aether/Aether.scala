package org.fusesource.insight.maven.aether

import java.{util => ju}
import java.util.Properties
import java.io.{FileInputStream, File}

import collection.JavaConversions._

import org.apache.maven.repository.internal.MavenRepositorySystemSession

import org.sonatype.aether.RepositorySystem
import org.sonatype.aether.RepositorySystemSession
import org.sonatype.aether.collection.CollectRequest
import org.sonatype.aether.graph.{Dependency, DependencyNode}
import org.sonatype.aether.util.artifact.DefaultArtifact
import org.sonatype.aether.util.graph.PreorderNodeListGenerator
import org.codehaus.plexus.DefaultPlexusContainer
import org.sonatype.aether.artifact.Artifact
import org.sonatype.aether.repository.{Authentication, RemoteRepository, LocalRepository}
import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import collection.mutable.ListBuffer
import org.sonatype.aether.resolution.ArtifactResolutionException
import org.apache.maven.model.Model
import java.net.URL
import org.codehaus.plexus.util.IOUtil


object Authentications {
  val home = System.getProperty("user.home", ".")
  val repoFile = new File(home, ".repo.fusesource.com.properties")

  def getFuseRepoAuthentication(): Authentication = {
    if (!repoFile.exists()) {
      throw new IllegalArgumentException("No file available at " + repoFile + " to contain the username and password to connect to the fusesource repo!")
    }
    val properties = new Properties()
    properties.load(new FileInputStream(repoFile))
    val username = properties.getProperty("username")
    val password = properties.getProperty("password")
    if (username == null) {
      throw new IllegalArgumentException("Missing 'username' in file: " + repoFile)
    }
    if (password == null) {
      throw new IllegalArgumentException("Missing 'password' in file: " + repoFile)
    }
    println("Using user " + username + " to access repo.fusesource.com")
    new Authentication(username, password)
  }
}


object Aether {
  def userRepository = System.getProperty("user.home", ".") + "/.m2/repository"

  var authorised = false

  def unauthorizedRepositories = List(
    Repository("proxy.fusesource.com", "https://repo.fusesource.com/nexus/content/groups/m2-proxy", Authentications.getFuseRepoAuthentication()),
    Repository("central", "http://repo2.maven.org/maven2/"),
    Repository("public.fusesource.com", "https://repo.fusesource.com/nexus/content/groups/public"),
    Repository("snapshots.fusesource.com", "https://repo.fusesource.com/nexus/content/groups/public-snapshots"),
    Repository("old.public.fusesource.com", "https://repo.fusesource.com/maven2"),
    Repository("old.public.fusesource.com", "https://repo.fusesource.com/maven2"),
    Repository("public.sonatype.com", "https://oss.sonatype.org/content/groups/public"),
    Repository("maven1.java.net", "http://download.java.net/maven/1"),
    //Repository("maven2.jboss.org", "http://repository.jboss.org/maven2"),
    Repository("com.springsource.repository.bundles.release", "http://repository.springsource.com/maven/bundles/release"),
    Repository("com.springsource.repository.bundles.external", "http://repository.springsource.com/maven/bundles/external"),
    Repository("com.springsource.repository.libraries.release", "http://repository.springsource.com/maven/libraries/release"),
    Repository("com.springsource.repository.libraries.external", "http://repository.springsource.com/maven/libraries/external")
  )

  def defaultRepositories = if (authorised) {
      Repository("proxy.fusesource.com", "https://repo.fusesource.com/nexus/content/groups/m2-proxy", Authentications.getFuseRepoAuthentication()) :: unauthorizedRepositories
  } else {
    unauthorizedRepositories
  }


  def artifact(node: DependencyNode) = node.getDependency.getArtifact

  def groupId(node: DependencyNode): String = artifact(node).getGroupId

  def artifactId(node: DependencyNode): String = artifact(node).getArtifactId

  def version(node: DependencyNode): String = artifact(node).getVersion

  def extension(node: DependencyNode): String = artifact(node).getExtension

  def classifier(node: DependencyNode): String = artifact(node).getClassifier

  def idLessVersion(node: DependencyNode): String = groupId(node) + ":" + artifactId(node) + ":" + extension(node) + ":" + classifier(node)
}

import Aether._

case class Repository(id: String, url: String, authentication: Authentication = null, repoType: String = "default") {
  def toRemoteRepository: RemoteRepository = {
    val repo = new RemoteRepository(id, repoType, url)
    if (authentication != null) {
      repo.setAuthentication(authentication)
    }
    repo
  }
}


class Aether(localRepoDir: String = userRepository, remoteRepos: List[Repository] = defaultRepositories) {


  val localRepository = new LocalRepository(localRepoDir)
  lazy val repositorySystem = newManualSystem()

  def newManualSystem(): RepositorySystem = {
    /*
        val locator = new DefaultServiceLocator()
        locator.setServices(classOf[WagonProvider], new ManualWagonProvider())
        locator.addService(classOf[RepositoryConnectorFactory], classOf[WagonRepositoryConnectorFactory])
        return locator.getService(classOf[RepositorySystem])
    */
    new DefaultPlexusContainer().lookup(classOf[RepositorySystem])

  }

  def newSession(): RepositorySystemSession = {
    val session = new MavenRepositorySystemSession()
    session.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(localRepository))

    session.setTransferListener(new ConsoleTransferListener(System.out))
    session.setRepositoryListener(new ConsoleRepositoryListener())

    // uncomment to generate dirty trees
    // session.setDependencyGraphTransformer( null )
    session
  }

  /**
   * Resolves a local build's pom and its dependencies
   */
  def resolveLocalProject(pomFile: File): AetherPomResult = {
    val model = loadPom(pomFile)
    resolveLocalProject(pomFile, model)
  }

  /**
   * If the model defines any repositories then create a child aether otherwise return the same paremt
   */
  def aether(model: Model): Aether = {
    val repos = model.getRepositories
    if (repos.isEmpty)
      this
    else {
      val list: List[Repository] = repos.map{ r => Repository(r.getId, r.getUrl)}.toList
      new Aether(userRepository, remoteRepos ++ list)
    }
  }

  /**
   * Resolves a local build from the root pom file
   */
  def resolveLocalProject(pomFile: File, model: Model): AetherPomResult = {
    val result = resolve(model)
    val modules = model.getModules
    var children = ListBuffer[AetherJarOrPom]()
    val rootDir = pomFile.getParentFile
    val childAether = aether(model)
    for (moduleName <- modules) {
      val childFile = new File(rootDir, moduleName + "/pom.xml")
      val childModel = childAether.loadPom(childFile)
      if (childModel.getGroupId == null) {
        childModel.setGroupId(model.getGroupId)
      }
      if (childModel.getVersion == null) {
        childModel.setVersion(model.getVersion)
      }
      println("resolving module: " + childModel.getGroupId + ":" + childModel.getArtifactId + ":" + childModel.getVersion)
      if (childModel.getPackaging == "pom") {
        children += childAether.resolveLocalProject(childFile, childModel)
      } else {
        children += childAether.resolve(childModel)
      }
    }
    AetherPomResult(result, children)
  }

  def resolve(model: Model): AetherResult = {
    val extension = model.getPackaging match {
      case "bundle" => "jar"
      case p => p
    }
    resolve(model.getGroupId, model.getArtifactId, model.getVersion, extension)
  }

  /**
   * Resolves a pom and its dependent modules
   */
  def resolvePom(groupId: String, artifactId: String, version: String): AetherPomResult = {
    val result = resolve(groupId, artifactId, version, "pom")
    val root = result.root
    val file = root.getDependency.getArtifact.getFile
    val model = loadPom(file)
    val modules = model.getModules
    var children = ListBuffer[AetherJarOrPom]()
    for (moduleName <- modules) {
      println("Found module " + moduleName)
      // we may be a jar or a pom
      var pomGroupId = model.getGroupId()
      if (pomGroupId == null || pomGroupId.isEmpty) {
        pomGroupId = groupId
      }
      try {
        val childPom = resolvePom(pomGroupId, moduleName, version)
        children += childPom
      } catch {
        case e: ArtifactResolutionException =>
          // lets try a jar
          try {
            val childPom = resolve(pomGroupId, moduleName, version)
            children += childPom
          } catch {
            case e2: Throwable =>
              println("Could be artifact id is not the same as the module name for " + e)
          }
      }
    }
    AetherPomResult(result, children)
  }

  def resolve(groupId: String, artifactId: String, version: String, extension: String = "jar", classifier: String = ""): AetherResult = {
    val session = newSession()
    val dependency = new Dependency(new DefaultArtifact(groupId, artifactId, classifier, extension, version), "runtime")

    val collectRequest = new CollectRequest()
    collectRequest.setRoot(dependency)
    for (repo <- remoteRepos) {
      collectRequest.addRepository(repo.toRemoteRepository)
    }

    val rootNode = repositorySystem.collectDependencies(session, collectRequest).getRoot()

    repositorySystem.resolveDependencies(session, rootNode, null)

    val nlg = new PreorderNodeListGenerator()
    rootNode.accept(nlg)

    AetherResult(rootNode, nlg.getFiles(), nlg.getClassPath())
  }

  /**
   * Loads a pom from the given file
   */
  def loadPom(file: File): Model = {
    new MavenXpp3Reader().read(new FileInputStream(file))
  }


  def compare(groupId: String, artifactId: String, version1: String, version2: String, extension: String = "jar", classifier: String = ""): CompareResult = {
    val result1 = resolve(groupId, artifactId, version1, extension, classifier)
    val result2 = resolve(groupId, artifactId, version2, extension, classifier)
    CompareResult(result1, result2)
  }

  def displayTree(node: DependencyNode, indent: String, sb: StringBuffer) {
    sb.append(indent + node.getDependency()).append("\n")
    val childIndent = indent + "  "
    for (child <- node.getChildren) {
      displayTree(child, childIndent, sb)
    }
  }

}

trait AetherJarOrPom {
  def dump: Unit
  def tree: String
  def root: DependencyNode
}

case class AetherResult(root: DependencyNode, resolvedFiles: ju.List[File], resolvedClassPath: String) extends AetherJarOrPom {

  def dump: Unit = {
    println("tree: " + tree)
  }

  def tree: String = {
    val dump = new StringBuffer()
    displayTree(root, "", dump)
    dump.toString

  }

  protected def displayTree(node: DependencyNode, indent: String, sb: StringBuffer) {
    sb.append(indent + node.getDependency()).append("\n")
    val childIndent = indent + "  "
    for (child <- node.getChildren) {
      displayTree(child, childIndent, sb)
    }
  }
}

case class AetherPomResult(result: AetherResult, modules: Traversable[AetherJarOrPom]) extends AetherJarOrPom {

  def dump: Unit = {
    result.dump
  }

  def root = result.root

  def tree = result.tree

  /*
  def dump: Unit = {
    println("tree: " + tree)
  }

  def tree: String = {
    val dump = new StringBuffer()
    displayTree(root, "", dump)
    dump.toString

  }

  protected def displayTree(node: DependencyNode, indent: String, sb: StringBuffer) {
    sb.append(indent + node.getDependency()).append("\n")
    val childIndent = indent + "  "
    for (child <- node.getChildren) {
      displayTree(child, childIndent, sb)
    }
  }
  */
}

case class CompareResult(result1: AetherResult, result2: AetherResult) {
  val root = CompareDependencyNode(Some(result1.root), Some(result2.root))
}

case class CompareDependencyNode(node1: Option[DependencyNode], node2: Option[DependencyNode]) {
  if (node1.isEmpty && node2.isEmpty) throw new IllegalArgumentException("Should have either node1 or node2 specified!")

  def groupId: String = Aether.groupId(node)

  def artifactId: String = Aether.artifactId(node)

  def extension: String = Aether.extension(node)

  def classifier: String = Aether.classifier(node)

  def version1: Option[String] = node1.map(Aether.version(_))

  def version2: Option[String] = node2.map(Aether.version(_))

  def change: VersionChange = version1 match {
    case Some(v) =>
      version2 match {
        case Some(v2) =>
          if (v == v2) {
            SameVersion(v)
          } else {
            UpdateVersion(v, v2)
          }
        case _ =>
          AddVersion(v)
      }
    case _ =>
      version2 match {
        case Some(v2) =>
          RemoveVersion(v2)
        case _ =>
          throw new IllegalArgumentException("Neither version has a value!")
      }
  }

  def artifact: Artifact = Aether.artifact(node)

  def dependency = node.getDependency

  def isOptional = dependency.isOptional

  def scope = dependency.getScope


  /**
   * returns either the first or second defined node
   */

  protected def node: DependencyNode = node1.getOrElse(node2.get)

  lazy val children = createChildren

  protected def createChildren: List[CompareDependencyNode] = {
    def toDependencyMap(optionalNode: Option[DependencyNode]): Map[String, DependencyNode] = {
      val children: List[DependencyNode] = optionalNode match {
        case Some(n) => n.getChildren.toList
        case _ => Nil
      }
      val entries = children.map{
          c =>
          val key = Aether.groupId(c) + ":" + Aether.artifactId(c)
          key -> c
      }
      Map(entries: _*)
    }

    val m1 = toDependencyMap(node1)
    val m2 = toDependencyMap(node2)
    val keys = m1.keySet ++ m2.keySet
    keys.map{
        k => CompareDependencyNode(m1.get(k), m2.get(k))
    }.toList
  }
}

abstract sealed class VersionChange {
  def summary: String

  def isAdd = false

  def isUpdate = false

  def isAddOrUpdate = isAdd || isUpdate
}

case class AddVersion(version: String) extends VersionChange {
  def summary = "+ " + version

  override def isAdd = true
}

case class RemoveVersion(version: String) extends VersionChange {
  def summary = "- " + version
}

case class SameVersion(version: String) extends VersionChange {
  def summary = "= " + version
}

case class UpdateVersion(version1: String, version2: String) extends VersionChange {
  def summary = version1 + " -> " + version2

  override def isUpdate = true
}

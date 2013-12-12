/*
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.camel.tooling.util

import org.apache.maven.cli.MavenCli
import org.fusesource.insight.maven.aether.Aether
import org.fusesource.scalate.test.FunSuiteSupport
import org.junit.Assert._
import java.io.{FileInputStream, File}
import org.fusesource.scalate.util.IOUtil
import scala.collection.JavaConversions._
import java.util

class ArchetypeTest extends FunSuiteSupport {
  val verbose = true

  var aether = new Aether()

  var groupId = "myGroup"
  var artifactId = "myArtifact"
  var packageName = "org.acme.mystuff"

  // lets get the versions from the pom.xml via a system property
  val camelVersion = System.getProperty("camel-version", "2.10.0.fuse-71-013")
  val projectVersion = System.getProperty("project.version", "2.10.0.fuse-71-013")
  val basedir = new File(System.getProperty("basedir", "."))

  val outDirs = new util.ArrayList[String]()

  test("generate activemq archetype") {
    assertArchetypeCreated("camel-archetype-activemq")
  }

  test("generate spring archetype") {
    assertArchetypeCreated("camel-archetype-spring")
  }

  test("generate java archetype") {
    assertArchetypeCreated("camel-archetype-java")
  }

  test("generate component archetype") {
    assertArchetypeCreated("camel-archetype-component")
  }

  test("generate dataformat archetype") {
    assertArchetypeCreated("camel-archetype-dataformat")
  }

  test("generate drools archetype") {
    assertArchetypeCreated("camel-drools-archetype", "io.fabric8", projectVersion,
      new File(basedir, "../archetypes/camel-drools-archetype/target/camel-drools-archetype-" + projectVersion + ".jar"))
  }

  protected def assertArchetypeCreated(artifactId: String, groupId: String = "org.apache.camel.archetypes",
                                       version: String = camelVersion): Unit = {
    val result = aether.resolve(groupId, artifactId, version)

    val files = result.resolvedFiles
    assertTrue("No files resolved for " + artifactId + " version: " + version, files.size > 0)
    val archetypejar = files.get(0)
    assertTrue("archetype jar does not exist", archetypejar.exists())


    assertArchetypeCreated(artifactId, groupId, version, archetypejar)
  }


  def assertArchetypeCreated(artifactId: String, groupId: String, version: String, archetypejar: File) {
    val outDir = new File(baseDir, "target/" + artifactId + "-output")

    println("Creating archetype " + groupId + ":" + artifactId + ":" + version)
    val properties = new
        ArchetypeHelper(new FileInputStream(archetypejar), outDir, groupId, artifactId, version)
      .parseProperties
    println("Has preferred properties: " + properties)

    val helper = new ArchetypeHelper(new FileInputStream(archetypejar), outDir, groupId, artifactId, version)
    helper.packageName = packageName

    // lets override some properties
    helper.overrideProperties = Map("slf4j-version" -> "1.5.0")
    helper.execute

    // expected pom file
    val pom = new File(outDir, "pom.xml")
    assertFileExists(pom)

    val pomText = IOUtil.loadTextFile(pom)
    val badText = "${camel-"
    if (pomText.contains(badText)) {
      if (verbose) {
        println(pomText)
      }
      fail("" + pom + " contains " + badText)
    }

    outDirs.add(outDir.getPath)
  }

  override def afterAll(): Unit = {
    super.afterAll()

    // now let invoke the projects
    for (outDir <- outDirs) {
      println("Invoking project in " + outDir)
      val maven = new MavenCli()
      val results = maven.doMain(Array("compile"), outDir, System.out, System.out)
      println("result: " + results)
      assertEquals("Build of project " + outDir + " failed. Result = " + results, 0, results)
    }
  }

  protected def assertFileExists(file: File): Unit = {
    assertTrue("file should exist: " + file, file.exists)
  }

}
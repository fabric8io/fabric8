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

import org.fusesource.scalate.test.FunSuiteSupport
import org.junit.Assert._
import java.io.{FileInputStream, File}
import org.fusesource.scalate.util.IOUtil

class ArchetypeTest extends FunSuiteSupport {
  val verbose = true

  var groupId = "myGroup"
  var artifactId = "myArtifact"
  var packageName = "org.acme.mystuff"

  // lets get the latest version from the pom.xml via a system property
  var version = System.getProperty("camel-version", "2.10.0.fuse-71-013")


  // TODO this test fails currently!!!
  ignore("generate activemq archetype") {
    assertArchetypeCreated("camel-archetype-activemq-")
  }

  test("generate spring archetype") {
    assertArchetypeCreated("camel-archetype-spring-")
  }

  test("generate java archetype") {
    assertArchetypeCreated("camel-archetype-java-")
  }

  test("generate component archetype") {
    assertArchetypeCreated("camel-archetype-component-")
  }

  test("generate dataformat archetype") {
    assertArchetypeCreated("camel-archetype-dataformat-")
  }

  protected def assertArchetypeCreated(archetypePrefix: String): Unit = {
    val outDir = new File(baseDir, "target/" + archetypePrefix + "output")

    // lets find an archtype
    val archetypesDir = new File(baseDir, "../../../ridersource/eclipse-tooling/org.fusesource.ide.branding/archetypes")
    assertFileExists(archetypesDir)
    assertTrue("should be directory: " + archetypesDir, archetypesDir.isDirectory)

    val archetypejar = archetypesDir.listFiles.find(_.getName.startsWith(archetypePrefix)).
            getOrElse(throw new Exception("Failed to find archetype!"))

    val properties = new ArchetypeHelper(new FileInputStream(archetypejar), outDir, groupId, artifactId, version).parseProperties
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
    outDir
  }


  protected def assertFileExists(file: File): Unit = {
    assertTrue("file should exist: " + file, file.exists)
  }

}
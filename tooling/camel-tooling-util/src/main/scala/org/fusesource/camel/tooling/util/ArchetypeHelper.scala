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

import java.{util => ju, lang => jl}
import java.util.zip.ZipInputStream
import java.util.regex.Pattern
import xml.XML
import collection.mutable.HashMap
import collection.{mutable => mu}
import org.fusesource.scalate.util.IOUtil
import org.fusesource.scalate.util.IOUtil._
import java.io._


class ArchetypeHelper(val archetypeIn: InputStream, val outputDir: File, val groupId: String, val artifactId: String, val version: String = "1.0-SNAPSHOT") {

  var packageName = ""
  var verbose: Boolean = false
  var createDefaultDirectories: Boolean = true

  var overrideProperties: Map[String, String] = Map()

  val zipEntryPrefix = "archetype-resources/"
  val binarySuffixes = List(".png", ".ico", ".gif", ".jpg", ".jpeg", ".bmp")

  protected val webInfResources = "src/main/webapp/WEB-INF/resources"
  protected val sourcePathRegexPattern = "(src/(main|test)/(java|scala)/)(.*)".r.pattern

  def execute: jl.Integer = {
    def info(s: => String = "") = println(s)
    def debug(s: => String) = if (verbose) println(s)

    outputDir.mkdirs()

    if (packageName == null || packageName.length == 0) {
      packageName = groupId + "." + artifactId
    }
    val packageDir = packageName.replace('.', '/')

    info("Creating archetype using maven groupId: " +
            groupId + " artifactId: " + artifactId + " version: " + version
            + " in directory: " + outputDir)

    var replaceProperties = new HashMap[String, String]

    using(new ZipInputStream(archetypeIn)) {
      zip =>
        var ok = true
        while (ok) {
          val entry = zip.getNextEntry
          if (entry == null) {
            ok = false
          } else {
            if (!entry.isDirectory) {
              val fullName = entry.getName
              if (fullName.startsWith(zipEntryPrefix)) {
                val name = replaceFileProperties(fullName.substring(zipEntryPrefix.length), replaceProperties)
                debug("processing resource: " + name)


                val idx = name.lastIndexOf('/')
                val matcher = sourcePathRegexPattern.matcher(name)
                val dirName = if (packageName.length > 0 && idx > 0 && matcher.matches) {
                  val prefix = matcher.group(1)
                  prefix + packageDir + "/" + name.substring(prefix.length)
                } else if (packageName.length > 0 && name.startsWith(webInfResources)) {
                  "src/main/webapp/WEB-INF/" + packageDir + "/resources" + name.substring(webInfResources.length)
                } else {
                  name
                }

                // lets replace properties...
                val file = new File(outputDir, dirName)
                file.getParentFile.mkdirs
                using(new FileOutputStream(file)) {
                  out =>
                    if (binarySuffixes.find(name.endsWith(_)).isDefined) {
                      // binary file?  don't transform.
                      copy(zip, out)
                    } else {
                      // text file...
                      val bos = new ByteArrayOutputStream()
                      copy(zip, bos)
                      val text = new String(bos.toByteArray, "UTF-8")
                      out.write(transformContents(text, replaceProperties).getBytes())
                    }
                }
              } else if (fullName == "META-INF/maven/archetype-metadata.xml") {
                parseReplaceProperties(zip, replaceProperties)
              }
            }
            zip.closeEntry
          }
        }
    }

    // override property values
    replaceProperties ++= overrideProperties

    println("Using replace properties: " + replaceProperties)

    // now lets replace all the properties in the pom.xml
    if (!replaceProperties.isEmpty) {
      val pom = new File(outputDir, "pom.xml")
      var text = IOUtil.loadTextFile(pom)
      for ((k, v) <- replaceProperties) {
        text = replaceVariable(text, k, v)
      }
      IOUtil.writeText(pom, text)
    }

    // now lets create the default directories
    if (createDefaultDirectories) {
      val srcDir = new File(outputDir, "src")
      val mainDir = new File(srcDir, "main")
      val testDir = new File(srcDir, "test")


      val srcDirName = if (new File(mainDir, "scala").exists || new File(testDir, "scala").exists)
        "scala"
      else "java"

      for (dir <- List(mainDir, testDir); name <- List(srcDirName + "/" + packageDir, "resources")) {
        new File(dir, name).mkdirs()
      }
    }

    return 0
  }


  def parseProperties: mu.Map[String, String] = {
    var replaceProperties = new HashMap[String, String]

    using(new ZipInputStream(archetypeIn)) {
      zip =>
        var ok = true
        while (ok) {
          val entry = zip.getNextEntry
          if (entry == null) {
            ok = false
          } else {
            if (!entry.isDirectory) {
              val fullName = entry.getName
              if (fullName == "META-INF/maven/archetype-metadata.xml") {
                parseReplaceProperties(zip, replaceProperties)
              }
            }
            zip.closeEntry
          }
        }
    }
    replaceProperties
  }


  protected def parseReplaceProperties(zip: ZipInputStream, replaceProperties: HashMap[String, String]) {
    val bos = new ByteArrayOutputStream()
    copy(zip, bos)
    val e = XML.load(new ByteArrayInputStream(bos.toByteArray))

    val properties = e \\ "requiredProperty"
    for (p <- properties) {
      val key = p.attribute("key").mkString("")
      val value = (p \ "defaultValue").text
      replaceProperties(key) = value
      if (key == "name" && value.isEmpty()) {
        replaceProperties(key) = "HelloWorld"
      }
    }
  }

  protected def transformContents(fileContents: String, replaceProperties: HashMap[String, String]): String = {
    var answer = removeInvalidHeaderComments(fileContents)
    answer = replaceVariable(answer, "package", packageName)
    answer = replaceVariable(answer, "packageName", packageName)
    answer = replaceAllVariable(answer, "groupId", groupId)
    answer = replaceAllVariable(answer, "artifactId", artifactId)
    answer = replaceAllVariable(answer, "version", version)
    for ((key, value) <- replaceProperties) {
      answer = replaceVariable(answer, key, value)
    }
    answer
  }

  protected def removeInvalidHeaderComments(text: String): String = {
    var answer = ""
    val lines = text.split("\n")
    for (line <- lines) {
      var l = line.trim
      if (!l.startsWith("##")) {
        answer = answer.concat(line)
        answer = answer.concat("\n")
      }
    }
    answer
  }

  protected def replaceFileProperties(fileName: String, replaceProperties: HashMap[String, String]): String = {
    var answer = fileName
    for ((key, value) <- replaceProperties) {
      answer = answer.replace("__" + key + "__", value)
    }
    answer
  }

  protected def replaceVariable(text: String, name: String, value: String): String = {
    if (value.contains('}')) {
      println("Ignoring dodgy value '" + value + "'")
      text
    } else {
      //println("Replacing '" + name + "' with '" + value + "'")
      text.replaceAll(Pattern.quote("${" + name + "}"), value)
    }
  }

  protected def replaceAllVariable(text: String, name: String, value: String): String = {
    var answer = ""
    answer = text.replaceAll(Pattern.quote("${" + name + "}"), value)
    answer = answer.replaceAll(Pattern.quote("$" + name), value)
    answer
  }
}
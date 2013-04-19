package org.fusesource.tooling.archetype.builder

import java.io.File
import kotlin.dom.*
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.Document
import org.xml.sax.InputSource
import java.io.StringReader
import java.io.FileWriter
import java.util.TreeSet

val sourceFileExtensions = hashSet(
        "bpmn",
        "drl",
        "html",
        "groovy",
        "jade",
        "java",
        "jbpm",
        "js",
        "json",
        "jsp",
        "kotlin",
        "ks",
        "md",
        "properties",
        "scala",
        "ssp",
        "ts",
        "txt",
        "xml"
)

val excludeExtensions = hashSet("iml", "iws", "ipr")

val sourceCodeDirNames = arrayList("java", "kotlin", "scala")

val sourceCodeDirPaths = (
sourceCodeDirNames.map { "src/main/$it" } +
sourceCodeDirNames.map { "src/test/$it" } +
arrayList("target", "build", "pom.xml", "archetype-metadata.xml")).toSet()

public open class ArchetypeBuilder() {
    public open fun configure(args: Array<String>): Unit {
    }

    public open fun generateArchetypes(sourceDir: File, outputDir: File): Unit {
        println("Generating archetypes from sourceDir: ${sourceDir.canonicalPath}")
        val files = sourceDir.listFiles()
        if (files != null) {
            for (file in files) {
                if (file.isDirectory()) {
                    var pom = File(file, "pom.xml")
                    if (pom.exists()) {
                        val outputName = file.name.replace("example", "archetype")
                        generateArchetype(file, pom, File(outputDir, outputName))
                    }
                }
            }
        }
    }

    protected open fun generateArchetype(directory: File, pom: File, outputDir: File): Unit {
        println("Generating archetype at ${outputDir.canonicalPath} from ${directory.canonicalPath}")
        val srcDir = File(directory, "src/main")
        val testDir = File(directory, "src/test")
        var archetypeOutputDir = File(outputDir, "src/main/resources/archetype-resources")
        var metadataXmlFile = File(directory, "archetype-metadata.xml")
        var metadataXmlOutFile = File(outputDir, "src/main/resources-filtered/META-INF/maven/archetype-metadata.xml")


        var replaceFunction = {(s: String) -> s }
        val mainSrcDir = sourceCodeDirNames.map{ File(srcDir, it) }.find { it.exists() }
        if (mainSrcDir != null) {
            // lets find the first directory which contains more than one child
            // to find the root-most package
            val rootPackage = findRootPackage(mainSrcDir)

            if (rootPackage != null) {
                val packagePath = mainSrcDir.relativePath(rootPackage)
                val packageName = packagePath.replaceAll("/", ".") // .replaceAll("/", "\\\\.")
                val regex = packageName.replaceAll("\\.", "\\\\.")

                replaceFunction = {(s: String) -> s.replaceAll(regex, "\\\${package}") }

                // lets recursively copy files replacing the package names
                val outputMainSrc = File(archetypeOutputDir, directory.relativePath(mainSrcDir))
                copyCodeFiles(rootPackage, outputMainSrc, replaceFunction)

                val testSrcDir = sourceCodeDirNames.map{ File(testDir, it) }.find { it.exists() }
                if (testSrcDir != null) {
                    val rootTestDir = File(testSrcDir, packagePath)
                    val outputTestSrc = File(archetypeOutputDir, directory.relativePath(testSrcDir))
                    if (rootTestDir.exists()) {
                        copyCodeFiles(rootTestDir, outputTestSrc, replaceFunction)
                    } else {
                        copyCodeFiles(testSrcDir, outputTestSrc, replaceFunction)
                    }
                }
            }
        }
        copyPom(pom, File(archetypeOutputDir, "pom.xml"), metadataXmlFile, metadataXmlOutFile, replaceFunction)

        // now lets copy all non-ignored files across
        copyOtherFiles(directory, directory, archetypeOutputDir, replaceFunction)
    }

    /**
     * Copies all java/kotlin/scala code
     */
    protected open fun copyCodeFiles(srcDir: File, outDir: File,replaceFn: (String) -> String): Unit {
        if (srcDir.isFile()) {
            copyFile(srcDir, outDir, replaceFn)
        } else {
            outDir.mkdirs()
            val names = srcDir.list()
            if (names != null) {
                for (name in names) {
                    copyCodeFiles(File(srcDir, name), File(outDir, name), replaceFn)
                }
            }
        }
    }

    protected open fun copyPom(pom: File, outFile: File, metadataXmlFile: File, metadataXmlOutFile: File, replaceFn: (String) -> String): Unit {
        val text = replaceFn(pom.readText())

        // lets update the XML
        val doc = parseXml(InputSource(StringReader(text)))
        val root = doc.documentElement
        // TODO would be more concise when this fixed http://youtrack.jetbrains.com/issue/KT-2922
        //val propertyNameSet = sortedSet<String>()
        val propertyNameSet: MutableSet<String> = sortedSet<String>()

        if (root != null) {
            // remove the parent element
            val parents = root.elements("parent")
            if (parents.notEmpty()) {
                root.removeChild(parents[0])
            }

            // lets find all the property names
            for (e in root.elements("*")) {
                val text = e.childrenText
                val prefix = "\${"
                if (text.startsWith(prefix)) {
                    val offset = prefix.size
                    val idx = text.indexOf('}', offset + 1)
                    if (idx > 0) {
                        val name = text.substring(offset, idx)
                        if (isValidRequiredPropertyName(name)) {
                            propertyNameSet.add(name)
                        }
                    }
                }
            }

            // now lets replace the contents of some elements (adding new elements if they are not present)
            val beforeNames = arrayList("artifactId", "version", "packaging", "name", "properties")
            replaceOrAddElementText(doc, root, "version", "\${version}", beforeNames)
            replaceOrAddElementText(doc, root, "artifactId", "\${artifactId}", beforeNames)
            replaceOrAddElementText(doc, root, "groupId", "\${groupId}", beforeNames)
        }
        outFile.getParentFile()?.mkdirs()
        doc.writeXmlString(FileWriter(outFile), true)

        // lets update the archetype-metadata.xml file
        val archetypeXmlText = if (metadataXmlFile.exists()) metadataXmlFile.readText() else defaultArchetypeXmlText()
        val archDoc = parseXml(InputSource(StringReader(archetypeXmlText)))
        val archRoot = archDoc.documentElement
        println("Found property names $propertyNameSet")
        if (archRoot != null) {
            // lets add all the properties
            val requiredProperties = replaceOrAddElement(archDoc, archRoot, "requiredProperties", arrayList("fileSets"))

            // lets add the various properties in
            for (propertyName in propertyNameSet) {
                requiredProperties.addText("\n    ")
                requiredProperties.addElement("requiredProperty") {
                    setAttribute("key", propertyName)
                    addText("\n      ")
                    addElement("defaultValue") {
                        addText("\${$propertyName}")
                    }
                    addText("\n    ")
                }
            }
            requiredProperties.addText("\n  ")
        }
        metadataXmlOutFile.getParentFile()?.mkdirs()
        archDoc.writeXmlString(FileWriter(metadataXmlOutFile), true)
    }

    protected fun replaceOrAddElementText(doc: Document, parent: Element, name: String, content: String, beforeNames: Iterable<String>): Element {
        val element = replaceOrAddElement(doc, parent, name, beforeNames)
        element.text = content
        return element
    }

    protected fun replaceOrAddElement(doc: Document, parent: Element, name: String, beforeNames: Iterable<String>): Element {
        val children = parent.children()
        val elements = children.filter { it.nodeName == name }
        val element = if (elements.isEmpty()) {
            val newElement = doc.createElement(name)!!
            var first: Node? = null;
            for (n in beforeNames) {
                first = findChild(parent, n)
                if (first != null) break
            }
/*
            val before = beforeNames.map{ n -> findChild(parent, n)}
            val first = before.first
*/
            val node: Node = if (first != null) first!! else parent.getFirstChild()!!
            val text = doc.createTextNode("\n  ")!!
            parent.insertBefore(text, node)
            parent.insertBefore(newElement, text)
            newElement
        } else {
            elements[0]
        }
        return element as Element
    }

    protected fun findChild(parent: Element, n: String): Node? {
        val children = parent.children()
        return children.find { it.nodeName == n }
    }

    protected fun copyFile(src: File, dest: File, replaceFn: (String) -> String): Unit {
        if (isSourceFile(src)) {
            val text = replaceFn(src.readText())
            dest.writeText(text)
        } else {
            println("Not a source dir as the extention is ${src.extension}")
            src.copyTo(dest)
        }
    }

    /**
     * Copies all other source files which are not excluded
     */
    protected open fun copyOtherFiles(projectDir: File, srcDir: File, outDir: File, replaceFn: (String) -> String): Unit {
        if (isValidFileToCopy(projectDir, srcDir)) {
            if (srcDir.isFile()) {
                copyFile(srcDir, outDir, replaceFn)
            } else {
                outDir.mkdirs()
                val names = srcDir.list()
                if (names != null) {
                    for (name in names) {
                        copyOtherFiles(projectDir, File(srcDir, name), File(outDir, name), replaceFn)
                    }
                }
            }
        }
    }


    protected open fun findRootPackage(directory: File): File? {
        val children = directory.listFiles { isValidSourceFileOrDir(it) }
        if (children != null) {
            val results = children.map { findRootPackage(it) }.filter { it != null }
            if (results.size == 1) {
                return results[0]
            } else {
                return directory
            }
        }
        return null
    }

    /**
     * Returns true if this file is a valid source file; so
     * excluding things like .svn directories and whatnot
     */
    protected open fun isValidSourceFileOrDir(file: File): Boolean {
        val name = file.name
        return !name.startsWith(".") && !excludeExtensions.contains(file.extension)
    }

    /**
     * Returns true if this file is a valid source file name
     */
    protected open fun isSourceFile(file: File): Boolean {
        val name = file.extension.toLowerCase()
        return sourceFileExtensions.contains(name)
    }

    /**
     * Is the file a valid file to copy (excludes files starting with a dot, build output
     * or java/kotlin/scala source code
     */
    protected open fun isValidFileToCopy(projectDir: File, src: File): Boolean {
        if (isValidSourceFileOrDir(src)) {
            if (src == projectDir) return true
            val relative = projectDir.relativePath(src)
            return !sourceCodeDirPaths.contains(relative)
        }
        return false
    }

    /**
     * Returns true if this is a valid archetype property name, so excluding
     * basedir and maven "project." names
     */
    protected open fun isValidRequiredPropertyName(name: String): Boolean {
        return name != "basedir" && !name.startsWith("project.")
    }


    protected open fun defaultArchetypeXmlText(): String = """<?xml version="1.0" encoding="UTF-8"?>
<archetype-descriptor xsi:schemaLocation="http://maven.apache.org/plugins/maven-archetype-plugin/archetype-descriptor/1.0.0 http://maven.apache.org/xsd/archetype-descriptor-1.0.0.xsd" name="camel-archetype-java"
    xmlns="http://maven.apache.org/plugins/maven-archetype-plugin/archetype-descriptor/1.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <requiredProperties/>
  <fileSets>
    <fileSet filtered="true" packaged="true" encoding="UTF-8">
      <directory>src/main/java</directory>
      <includes>
        <include>**/*.java</include>
      </includes>
    </fileSet>
    <fileSet filtered="true" encoding="UTF-8">
      <directory>src/main/resources</directory>
      <includes>
        <include>**/*.bpm*</include>
        <include>**/*.drl</include>
        <include>**/*.wsdl</include>
        <include>**/*.xml</include>
        <include>**/*.properties</include>
      </includes>
    </fileSet>
    <fileSet filtered="true" packaged="true" encoding="UTF-8">
      <directory>src/test/java</directory>
      <includes>
        <include>**/*.java</include>
      </includes>
    </fileSet>
    <fileSet filtered="true" encoding="UTF-8">
      <directory>src/test/resources</directory>
      <includes>
        <include>**/*.xml</include>
        <include>**/*.properties</include>
      </includes>
    </fileSet>
    <fileSet filtered="true" encoding="UTF-8">
      <directory>src/data</directory>
      <includes>
        <include>**/*.xml</include>
      </includes>
    </fileSet>
    <fileSet filtered="true" encoding="UTF-8">
      <directory></directory>
      <includes>
        <include>ReadMe.*</include>
      </includes>
    </fileSet>
  </fileSets>
</archetype-descriptor>
"""
}

package org.fusesource.tooling.archetype.builder

import java.io.File

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

val sourceCodeDirPaths = (sourceCodeDirNames.map { "src/main/$it" } + sourceCodeDirNames.map { "src/test/$it" }).toSet()

public open class ArchetypeBuilder() {
    public open fun configure(args: Array<String>): Unit {
    }

    public open fun generateArchetypes(sourceDir: File, outputDir: File): Unit {
        println("Generating archetypes from sourceDir: $sourceDir")
        val files = sourceDir.listFiles()
        if (files != null) {
            for (file in files) {
                if (file.isDirectory()) {
                    var pom = File(file, "pom.xml")
                    if (pom.exists()) {
                        val outputName = file.name.replace("example", "archetype")
                        generateArchetype(file, File(outputDir, outputName))
                    }
                }
            }
        }
    }

    protected open fun generateArchetype(directory: File, outputDir: File): Unit {
        println("Generating archetype at $outputDir from $directory")
        val srcDir = File(directory, "src/main")
        val testDir = File(directory, "src/test")
        var archetypeOutputDir = File(outputDir, "src/main/resources/archetype-resources")


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

        // now lets copy all non-ignored files across
        copyOtherFiles(directory, directory, archetypeOutputDir, replaceFunction)
    }

    /**
     * Copies all java/kotlin/scala code
     */
    protected open fun copyCodeFiles(srcDir: File, outDir: File, replaceFn: (String) -> String): Unit {
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

    protected fun copyFile(srcDir: File, outDir: File, replaceFn: (String) -> String): Unit {
        if (isSourceFile(srcDir)) {
            val text = replaceFn(srcDir.readText())
            outDir.writeText(text)
        } else {
            println("Not a source dir as the extention is ${srcDir.extension}")
            srcDir.copyTo(outDir)
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
            return relative != "target" && relative != "build" && !sourceCodeDirPaths.contains(relative)
        }
        return false
    }
}

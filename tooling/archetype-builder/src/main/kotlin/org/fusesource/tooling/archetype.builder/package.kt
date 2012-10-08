package org.fusesource.tooling.archetype.builder

import java.io.File

public fun main(args: Array<String>): Unit {
    val basedir = System.getProperty("basedir") ?: "."
    val srcDir = File(basedir, "../examples").getCanonicalFile()
    val outputDir = if (args.size > 0) args[0] else "../archetypes"
    val builder = ArchetypeBuilder()
    builder.configure(args)
    builder.generateArchetypes(srcDir, File(outputDir))
}

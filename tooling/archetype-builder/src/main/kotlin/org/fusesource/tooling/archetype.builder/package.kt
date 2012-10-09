package org.fusesource.tooling.archetype.builder

import java.io.File

public fun main(args: Array<String>): Unit {
    val basedir = System.getProperty("basedir") ?: "."
    val srcDir = File(basedir, "../examples").getCanonicalFile()
    val outputDir = if (args.size > 0) File(args[0]) else File(basedir, "../archetypes")
    val builder = ArchetypeBuilder()
    builder.configure(args)
    builder.generateArchetypes(srcDir, outputDir)
}

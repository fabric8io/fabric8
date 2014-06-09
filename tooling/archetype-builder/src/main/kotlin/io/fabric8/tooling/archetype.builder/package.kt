package io.fabric8.tooling.archetype.builder

import java.io.File

public fun main(args: Array<String>): Unit {
    try {
        val basedir = System.getProperty("basedir") ?: "."
        val catalogFile = File(basedir, "target/archetype-catalog.xml").getCanonicalFile()
        val quickStartSrcDir = File(basedir, "../../quickstarts").getCanonicalFile()
        val quickStartBeginnerSrcDir = File(basedir, "../../quickstarts/beginner").getCanonicalFile()
        val outputDir = if (args.size > 0) File(args[0]) else File(basedir, "../archetypes")
        val builder = ArchetypeBuilder(catalogFile)
        builder.configure(args)
        try {
            builder.generateArchetypes(quickStartSrcDir, outputDir)
            builder.generateArchetypes(quickStartBeginnerSrcDir, outputDir)
        } finally {
            println("Completed the generation. Closing!")
            builder.close()
        }
    } catch (e: Exception) {
        println("Caught: " + e)
        e.printStackTrace()
    }
}
    

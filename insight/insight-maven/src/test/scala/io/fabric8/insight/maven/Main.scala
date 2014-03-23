package io.fabric8.insight.maven

import aether.Aether

object Main {


  def main(args: Array[String]): Unit = {

    val aether = new Aether()

    aether.resolve("org.apache.camel", "camel-core", "2.13.0").dump
  }
}
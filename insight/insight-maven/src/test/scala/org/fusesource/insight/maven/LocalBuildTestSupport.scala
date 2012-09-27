package org.fusesource.insight.maven

import org.fusesource.insight.maven.aether._

import aether.Aether
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

/**
 * Base class for tests which work on local builds; resolving using the local mvn repo first
 */
@RunWith(classOf[JUnitRunner])
abstract class LocalBuildTestSupport extends FunSuite {

  var aether = new Aether(Aether.userRepository, List(Repository("local-repo", Aether.userRepository)) ++ Aether.defaultRepositories)

}
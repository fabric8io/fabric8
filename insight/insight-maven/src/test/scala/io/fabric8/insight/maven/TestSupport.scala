package io.fabric8.insight.maven

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import io.fabric8.insight.maven.aether.Aether

/**
 * Base class for tests
 */
@RunWith(classOf[JUnitRunner])
abstract class TestSupport extends FunSuite {

  var aether = new Aether()

}
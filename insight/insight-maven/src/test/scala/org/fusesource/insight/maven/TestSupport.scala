package org.fusesource.insight.maven

import aether.Aether
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

/**
 * Base class for tests
 */
@RunWith(classOf[JUnitRunner])
abstract class TestSupport extends FunSuite {

  var aether = new Aether()

}
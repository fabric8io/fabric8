package org.fusesource.insight.maven

import org.fusesource.scalate.test.{WebDriverMixin, WebServerMixin}
import org.junit.runner.RunWith
import org.junit.Ignore
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite

/**
 * Unit test for My Ap
 */
@RunWith(classOf[JUnitRunner])
class AppTest extends FunSuite with WebServerMixin with WebDriverMixin {

  test("home page") {
/*
    TODO - need to add the extra web app source directory
     
    webDriver.get(rootUrl + "index.html")
    pageContains("Scalate")
*/
  }

  //
  // TODO here is a sample test case for a page
  //
  // testPageContains("foo.scaml", "this is some content I expect")
}

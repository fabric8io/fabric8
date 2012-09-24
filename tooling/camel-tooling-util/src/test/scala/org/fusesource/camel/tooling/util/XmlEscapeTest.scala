/*
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.camel.tooling.util

import org.fusesource.scalate.test.FunSuiteSupport
import org.junit.Assert._
import java.io.{FileInputStream, File}
import org.fusesource.scalate.util.IOUtil

class XmlEscapeTest extends FunSuiteSupport {

  var verbose = true

  test("XML escape") {
    roundTrip("this is plain text")
    roundTrip("""<hello id="foo">some text</hello>""")
  }

  test("Xml unescape") {
    val text = "&lt;hello id=&quot;a&quot;&gt;world!&lt;/hello&gt;"
    val actual = XmlHelper.unescape(text)
    println("text:     " + text)
    println("unescape: " + actual)
  }

  protected def roundTrip(text: String): Unit = {
    val escaped = XmlHelper.escape(text)
    val actual = XmlHelper.unescape(escaped)

    if (verbose) {
      println("text:     " + text)
      println("escape:   " + escaped)
      println("unescape: " + actual)
    }

    expect(text) {actual}
  }
}
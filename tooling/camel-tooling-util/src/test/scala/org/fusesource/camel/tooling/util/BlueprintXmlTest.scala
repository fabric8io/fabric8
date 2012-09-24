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

import java.io.File
import org.junit.Assert._

class BlueprintXmlTest extends RouteXmlTestSupport {

  test("parses valid XML file") {
    val x = assertRoutes(new File(baseDir, "src/test/resources/blueprint.xml"), 1, CamelNamespaces.blueprintNS)

    val uris = x.endpointUris
    expect(1, "endpoint uris " + uris){ uris.size }
    assertTrue(uris.contains("seda:myConfiguredEndpoint"))
  }

  test("parses empty XML file") {
    val x = assertRoutes(new File(baseDir, "src/main/resources/org/fusesource/camel/tooling/blueprint-exemplar.xml"), 0, CamelNamespaces.blueprintNS)
  }

}
/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.insight.maven.aether

import org.apache.maven.wagon.Wagon
import org.apache.maven.wagon.providers.file.FileWagon
import org.sonatype.aether.connector.wagon.WagonProvider
import org.apache.maven.wagon.providers.http.LightweightHttpWagon

class ManualWagonProvider extends WagonProvider {

  def lookup(roleHint: String) = roleHint match {
    case "file" => new FileWagon()
    case "http" => new LightweightHttpWagon()
    case _ => null
  }

  def release(wagon: Wagon) = {
  }
}
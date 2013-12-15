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
package io.fabric8.monitor.plugins.jmx

import java.util.ArrayList
import java.util.List
import io.fabric8.monitor.api.DataSourceDTO

/**
 * Represents a tree of data source values
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class DataSourceGroup(var id: String=null, var name: String=null ) {

  var description: String = null
  var children: List[DataSourceGroup] = new ArrayList[DataSourceGroup]
  var data_sources: List[DataSourceDTO] = new ArrayList[DataSourceDTO]

  def dump(indent: Int, concise: Boolean) {
    import collection.JavaConversions._

    def print_indent(indent: Int) = for (i <- 0.to(indent)) {
      print("  ")
    }

    print_indent(indent)
    println(if (concise) id else this)

    val new_indent = indent + 1
    for (child <- children) {
      child.dump(new_indent, concise)
    }
    for (ds <- data_sources) {
      print_indent(new_indent)
      println(if (concise) ds.id else ds)
    }
  }

}

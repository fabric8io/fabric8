/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.monitor.plugins.jmx

import org.fusesource.fabric.monitor.api.DataSourceDTO
import java.util.ArrayList
import java.util.List

/**
 * Represents a tree of data source values
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class DataSourceGroup(var id: String=null, var name: String=null ) {

  var description: String = null
  var children: List[DataSourceGroup] = new ArrayList[DataSourceGroup]
  var data_sources: List[DataSourceDTO] = new ArrayList[DataSourceDTO]

}
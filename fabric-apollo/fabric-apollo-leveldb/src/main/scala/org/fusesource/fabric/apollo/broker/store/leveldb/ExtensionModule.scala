/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.apollo.broker.store.leveldb
import org.apache.activemq.apollo.util.Module

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class ExtensionModule extends Module {
  override def xml_packages = Array("org.fusesource.fabric.apollo.broker.store.leveldb.dto")
}
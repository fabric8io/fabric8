/**
 * Copyright (C) 2010-2012, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.apache.activemq.apollo.mqtt

import org.apache.activemq.apollo.util.JaxbModule

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class ExtensionJaxbModule extends JaxbModule {
  def xml_package = "org.apache.activemq.apollo.mqtt.dto"
}
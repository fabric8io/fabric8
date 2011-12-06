/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.monitor.internal

import javax.xml.bind.Unmarshaller
import javax.xml.stream.XMLStreamReader
import javax.xml.stream.util.StreamReaderDelegate
import java.io.InputStream
import java.util.Properties
import org.fusesource.fabric.api.monitor.XmlCodec
import java.net.URL

object PropertiesFilter {

  import XmlCodec._

  def decode[T](t : Class[T], url: URL, props: Properties): T = {
    return decode(t, url.openStream, props)
  }

  def decode[T](t: Class[T], is: InputStream, props: Properties): T = {
    if (is == null) {
      throw new IllegalArgumentException("input stream was null")
    }
    try {
      var reader: XMLStreamReader = factory.createXMLStreamReader(is)
      if (props != null) {
        reader = new PropertiesFilter(reader, props)
      }
      var unmarshaller: Unmarshaller = context.createUnmarshaller
      return t.cast(unmarshaller.unmarshal(reader))
    }
    finally {
      is.close
    }
  }
}

/**
 * Changes ${property} with values from a properties object
 */
class PropertiesFilter(parent: XMLStreamReader, val props: Properties) extends StreamReaderDelegate(parent) {
  override def getAttributeValue(index: Int): String = {
    import FilterSupport._
    return filter(super.getAttributeValue(index), props)
  }
}

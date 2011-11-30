/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.api.monitor

import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller
import javax.xml.bind.Unmarshaller
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamReader
import javax.xml.stream.util.StreamReaderDelegate
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.net.URL
import java.util.Properties

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
object XmlCodec {

  val factory: XMLInputFactory = XMLInputFactory.newInstance
  @volatile
  var _context: JAXBContext = null



  def context: JAXBContext = {
    var rc: JAXBContext = _context
    if (rc == null) {
      rc = ({
        _context = createContext; _context
      })
    }
    return rc
  }

  private def createContext: JAXBContext = {
    var packages: String = "org.fusesource.fabric.launcher.api"
    return JAXBContext.newInstance(packages)
  }

  def decode[T](t : Class[T], url: URL): T = {
    return decode(t, url.openStream())
  }

  def decode[T](t : Class[T], is: InputStream): T = {
    if (is == null) {
      throw new IllegalArgumentException("input stream was null")
    }
    try {
      var reader: XMLStreamReader = factory.createXMLStreamReader(is)
      var unmarshaller: Unmarshaller = context.createUnmarshaller
      return t.cast(unmarshaller.unmarshal(reader))
    }
    finally {
      is.close
    }
  }

  def encode[T](in: T, os: OutputStream, format: Boolean): Unit = {
    var marshaller: Marshaller = context.createMarshaller
    if (format) {
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
    }
    marshaller.marshal(in, new OutputStreamWriter(os))
  }


}
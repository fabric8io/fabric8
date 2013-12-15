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

package io.fabric8.monitor.internal

import javax.xml.bind.Unmarshaller
import javax.xml.stream.XMLStreamReader
import javax.xml.stream.util.StreamReaderDelegate
import java.io.InputStream
import java.util.Properties
import java.net.URL
import io.fabric8.monitor.api.XmlCodec

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

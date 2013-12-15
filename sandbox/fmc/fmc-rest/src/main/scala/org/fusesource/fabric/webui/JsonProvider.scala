/*
 * Copyright 2010 Red Hat, Inc.
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
package io.fabric8.webui

import org.codehaus.jackson.jaxrs.JacksonJsonProvider
import org.codehaus.jackson.map.annotate.JsonSerialize
import org.codehaus.jackson.map.{ObjectMapper, SerializationConfig}
import java.lang.reflect.Type
import java.lang.annotation.Annotation
import javax.ws.rs.core.{MultivaluedMap, MediaType}
import java.io.{Closeable, OutputStream, InputStream}
import javax.ws.rs.ext.{Provider, MessageBodyWriter}
import javax.ws.rs.{Produces, Consumes}
import io.fabric8.api.Container
import io.fabric8.api.CreateContainerMetadata
import io.fabric8.api.CreateContainerOptions
import org.codehaus.jackson.annotate.JsonIgnore;

abstract class MetadataIgnoreGetContainer {
  @JsonIgnore
  def getContainer(): Container

  @JsonIgnore
  def getCreateOptions[T <: CreateContainerOptions](): T
}


object JsonProvider {
  val mapper = new ObjectMapper();
  mapper.getSerializationConfig.set(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false)
  mapper.getSerializationConfig.setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
  mapper.getSerializationConfig.addMixInAnnotations(classOf[CreateContainerMetadata[_ <: CreateContainerOptions]], classOf[MetadataIgnoreGetContainer])

}

class JsonProvider extends JacksonJsonProvider(JsonProvider.mapper) {

}

@Provider
@Produces(Array(MediaType.WILDCARD, MediaType.APPLICATION_JSON))
class InputStreamProvider extends MessageBodyWriter[InputStream] {

  def isWriteable(kind: Class[_], genericType: Type, annotations: Array[Annotation], mediaType: MediaType) = {
    val rc = classOf[InputStream].isAssignableFrom(kind)
    rc
  }

  def getSize(is: InputStream, kind: Class[_], genericType: Type, annotations: Array[Annotation], mediaType: MediaType) = -1L

  def writeTo(is: InputStream, kind: Class[_], genericType: Type, annotations: Array[Annotation], mt: MediaType, headers: MultivaluedMap[String, AnyRef], os: OutputStream) {

    def close(c: Closeable) = try {
      c.close
    } catch {
      case _ =>
    }

    try {
      val buf = new Array[Byte](8192)
      var c = is.read(buf)
      while (c > 0) {
        os.write(buf, 0, c)
        c = is.read(buf)
      }
    } finally {
      close(is);
      close(os)
    }
  }
}

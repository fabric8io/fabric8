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
package io.fabric8.webui.profile

import io.fabric8.api.Profile
import io.fabric8.api.Container
import javax.ws.rs._
import javax.ws.rs.core.MediaType
import org.codehaus.jackson.annotate.JsonProperty
import collection.mutable.HashMap
import collection.JavaConversions._
import io.fabric8.webui._
import scala.Some
import scala.Some
import scala.Some
import io.fabric8.webui.{Services, Exportable, HasID, BaseResource}
import java.io._
import scala.concurrent.ops._
import org.apache.commons.compress.archivers.zip.{ZipArchiveEntry, ZipArchiveOutputStream}
import org.apache.commons.io.IOUtils
import java.util.{Date, Properties}
import java.net.URL


class PutParentsDTO {
  @JsonProperty
  var parents: Array[String] = _
}

class SetAttributeDTO {
  @JsonProperty
  var key:String = _

  @JsonProperty
  var value:String = _
}

class FeaturesRepositoryResource(_id: String, _xml: String, var _error: String) extends BaseResource with HasID {

  @JsonProperty
  def id = _id

  @JsonProperty
  def xml = _xml

  @JsonProperty
  def error = _error
}

class ProfileResource(val self: Profile, val container:Container = null ) extends BaseResource with HasID with Exportable {

  @JsonProperty
  def id = self.getId

  def agent_keys_with_prefix(prefix: String) = {
    var map = self.getConfigurations.getOrElseUpdate("io.fabric8.agent", Map[String, String]())
    map = map.filterKeys(_.startsWith(prefix))
    new ConfigurationResource(self, "io.fabric8.agent", map)
  }

  def write_to_zip(zip: ZipArchiveOutputStream) = {
    val configs = self.getFileConfigurations

    val root = new ZipArchiveEntry(id + File.separator)

    Services.LOG.debug("Adding directory {}", root)

    zip.putArchiveEntry(root)

    val attributes = self.getAttributes();
    val props = new Properties();
    attributes.foreach(p => props.setProperty(p._1, p._2))
    val out = new ByteArrayOutputStream
    props.store(out, "Exported on " + new Date)
    configs.put("attributes.properties", out.toByteArray)

    configs.foreach {
      case (key: String, data: Array[Byte]) =>
        val entry = new ZipArchiveEntry(id + File.separator + key)
        entry.setSize(data.length)
        Services.LOG.debug("Adding file {}", entry)
        zip.putArchiveEntry(entry)
        zip.write(data)
        zip.flush()
        zip.closeArchiveEntry()
    }

  }

  @GET
  @Path("export/{name}.zip")
  @Produces(Array("application/x-zip-compressed"))
  override def do_export(@PathParam("name") name: String) = super.do_export(name)

  def export(out: OutputStream): Unit = {
    val temp = File.createTempFile("exp", "zip")
    val zip = new ZipArchiveOutputStream(temp)
    write_to_zip(zip)
    zip.flush()
    zip.close()

    val in = new BufferedInputStream(new FileInputStream(temp))
    IOUtils.copy(in, out)
    in.close
    temp.delete
  }

  @JsonProperty
  def version = self.getVersion

  @JsonProperty
  def is_abstract = Option(profile_attributes.get(Profile.ABSTRACT)).getOrElse({"false"}).toBoolean

  @JsonProperty
  def is_locked = Option(profile_attributes.get(Profile.LOCKED)).getOrElse({"false"}).toBoolean

  @JsonProperty
  def is_hidden = Option(profile_attributes.get(Profile.HIDDEN)).getOrElse({"false"}).toBoolean

  @JsonProperty
  def description = Option(profile_attributes.get(Profile.DESCRIPTION)).getOrElse({""})

  @JsonProperty
  def profile_attributes = self.getAttributes

  @POST
  @Path("set_attribute")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  def set_attribute(attribute:SetAttributeDTO):Unit = self.setAttribute(attribute.key, attribute.value)

  @JsonProperty
  def children = fabric_service.getVersion(self.getVersion).getProfiles.filter(_.getParents.iterator.contains(self)).map(_.getId)

  @JsonProperty
  def agents = self.getAssociatedContainers.map(_.getId)

  @JsonProperty
  def system_props = system_props_resource.entries

  @Path("system_props")
  def system_props_resource = agent_keys_with_prefix("system.")

  @PUT
  @Path("system_props/{id}")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  def add_system_prop(config: CreateConfigurationEntryDTO) = add_agent_config(config)

  @DELETE
  @Path("system_props/{id}")
  def delete_system_prop(@PathParam("id") id: String) = delete_agent_config(id)

  @JsonProperty
  def config_props = config_props_resource.entries

  @Path("config_props")
  def config_props_resource = agent_keys_with_prefix("config.")

  @PUT
  @Path("config_props/{id}")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  def add_config_prop(config: CreateConfigurationEntryDTO) = add_agent_config(config)

  @DELETE
  @Path("config_props/{id}")
  def delete_config_prop(@PathParam("id") id: String) = delete_agent_config(id)

  @JsonProperty
  def bundles = bundles_resource.entries

  @Path("bundles")
  def bundles_resource = agent_keys_with_prefix("bundle.")

  @PUT
  @Path("bundles/{id}")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  def add_bundle(config: CreateConfigurationEntryDTO) = add_agent_config(config)

  @DELETE
  @Path("bundles/{id}")
  def delete_bundle(@PathParam("id") id: String) = delete_agent_config(id)

  @JsonProperty
  def fabs = fabs_resource.entries

  @Path("fabs")
  def fabs_resource = agent_keys_with_prefix("fab.")

  @PUT
  @Path("fabs/{id}")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  def add_fab(config: CreateConfigurationEntryDTO) = add_agent_config(config)

  @DELETE
  @Path("fabs/{id}")
  def delete_fab(@PathParam("id") id: String) = delete_agent_config(id)

  @JsonProperty
  def features = features_resource.entries

  @Path("features")
  def features_resource = agent_keys_with_prefix("feature.")

  @PUT
  @Path("features/{id}")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  def add_feature(config: CreateConfigurationEntryDTO) = add_agent_config(config)

  @DELETE
  @Path("features/{id}")
  def delete_feature(@PathParam("id") id: String) = delete_agent_config(id)

  @GET
  @Path("available_repos")
  def available_repos: Array[FeaturesRepositoryResource] = {
    self.getOverlay.getRepositories.map((x) => {
      var in:InputStream = null
      try {
        val url = new URL(x)
        in = url.openStream
        val lines = IOUtils.toString(in)
        new FeaturesRepositoryResource(x, lines , null)
      } catch {
        case t: Throwable =>
          Services.LOG.info("Error fetching features repository", t)
          new FeaturesRepositoryResource(x, null, new String(t.getMessage))
      } finally {
        Option(in).foreach(_.close())
      }
    }).toArray
  }

  @GET
  @Path("available_repos/{id}")
  def available_repo(@PathParam("id") id: String): FeaturesRepositoryResource = available_repos.find(_.id == id) getOrElse not_found

  @JsonProperty
  def repositories = repositories_resource.entries

  @Path("repositories")
  def repositories_resource = agent_keys_with_prefix("repository.")

  @PUT
  @Path("repositories/{id}")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  def add_repository(config: CreateConfigurationEntryDTO) = add_agent_config(config)

  @DELETE
  @Path("repositories/{id}")
  def delete_repository(@PathParam("id") id: String) = delete_agent_config(id)

  @JsonProperty
  def configurations = configurations_resource.entries

  @Path("configurations")
  def configurations_resource = new PropertyFilesResource(self)

  @PUT
  @Path("configurations/{id}")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  def add_configuration(config: CreatePropertyEntryDTO) = {
    val map = self.getFileConfigurations
    map.put(config.id, config.value)
    self.setFileConfigurations(map)
  }

  @DELETE
  @Path("configurations/{id}")
  def delete_configuration(@PathParam("id") id: String) = {
    val map = self.getFileConfigurations
    map.remove(id)
    self.setFileConfigurations(map)
  }

  @JsonProperty
  def parents = self.getParents map (_.getId)

  @POST
  @Path("parents")
  def set_parents(dto: PutParentsDTO) = {
    val version_id = self.getVersion
    val version = fabric_service.getVersion(version_id)
    val new_parents = dto.parents.map(version.getProfile(_))
    self.setParents(new_parents)
  }

  def add_agent_config(config: CreateConfigurationEntryDTO) = {
    val map = self.getConfigurations
    if (!map.containsKey("io.fabric8.agent")) {
      map.put("io.fabric8.agent", new HashMap[String, String])
    }
    map.get("io.fabric8.agent").put(config.id, config.value)
    self.setConfigurations(map)
  }

  def delete_agent_config(id: String) = {
    val map = self.getConfigurations
    if (map.containsKey("io.fabric8.agent")) {
      map.get("io.fabric8.agent").remove(id)
      self.setConfigurations(map)
    }
  }

}

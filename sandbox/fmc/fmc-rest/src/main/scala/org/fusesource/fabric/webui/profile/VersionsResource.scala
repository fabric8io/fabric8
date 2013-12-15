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

import javax.ws.rs._
import javax.ws.rs.core.Response.Status._
import org.codehaus.jackson.annotate.JsonProperty
import io.fabric8.webui._
import scala.Some
import scala.Some
import io.fabric8.webui.{Services, ByID, BaseResource}
import com.sun.jersey.multipart.FormDataParam
import java.io.{FileOutputStream, File, InputStream}
import com.sun.jersey.core.header.FormDataContentDisposition
import scala.Some
import java.util
import java.io._
import org.apache.commons.io.IOUtils
import org.apache.commons.compress.archivers.zip.ZipFile
import scala.Some
import io.fabric8.webui.patching.BaseUpgradeResource

class CreateVersionDTO {
  @JsonProperty
  var id: String = _
  @JsonProperty
  var derived_from: String = _
}

class DeleteVersionsDTO {
  @JsonProperty
  var ids: Array[String] = _
}

class SetDefaultVersionDTO {
  @JsonProperty
  var id: String = _
}

@Path("/versions")
class VersionsResource extends BaseResource {

  @GET
  override def get: Array[VersionResource] = {
    fabric_service.getVersions.map(new VersionResource(_)).sortWith(ByID(_, _))
  }

  @Path("{id}")
  def get(@PathParam("id") id: String): VersionResource = {
    val rc = get.find(_.id == id)
    rc getOrElse not_found
  }

  @POST
  @Path("import")
  @Consumes(Array("multipart/form-data"))
  @Produces(Array("text/html"))
  def import_version( @FormDataParam("target-name") target_name: String,
                      @FormDataParam("import-file") file: InputStream,
                      @FormDataParam("import-file") file_detail: FormDataContentDisposition): String = {

    val filename = file_detail.getFileName
    if (!filename.endsWith(".zip")) {
      respond(BAD_REQUEST, "Profile must be stored in a .zip file")
    }

    var name = filename.replace(".zip", "")
    Services.LOG.debug("Received file : {}", filename)

    val tmp = File.createTempFile("imp", ".zip")
    tmp.deleteOnExit()
    val fout = new FileOutputStream(tmp)
    IOUtils.copy(file, fout)
    fout.close

    val zip = new ZipFile(tmp)

    val profiles = new util.HashMap[String, util.HashMap[String, Array[Byte]]]

    import collection.JavaConverters._

    def get_profile(name: String) = {
      Option(profiles.get(name)) match {
        case Some(data) =>
          data
        case None =>
          profiles.put(name, new util.HashMap[String, Array[Byte]]())
          profiles.get(name)
      }

    }

    zip.getEntries.asScala.foreach((x) => {
      if (x.isDirectory()) {
        val profile_name = x.getName.replace("/", "")
        get_profile(profile_name)
      } else {

        val Array(profile, property_name, _*) = x.getName.split("/")

        Services.LOG.debug("Found entry profile: {}, property: {}", profile, property_name)
        Services.LOG.debug("Entry is (supposedly) {} bytes", x.getSize)

        val in = new BufferedInputStream(zip.getInputStream(x))
        val buffer = IOUtils.toByteArray(in);
        in.close()

        Services.LOG.debug("Read {} bytes", buffer.length)
        profiles.get(profile).put(property_name, buffer)
      }
    })

    zip.close
    tmp.delete

    val version = if (target_name.equals("")) {
      val rc = BaseUpgradeResource.create_version(BaseUpgradeResource.last_version_id)
      Services.LOG.info("Creating new version {}", rc.getId());
      rc
    } else {
      try {
        Option(fabric_service.getVersion(target_name)) match {
          case Some(rc) =>
            Services.LOG.info("Overwriting existing version {}", rc.getId());
            rc
          case None =>
            Services.LOG.info("Creating new emtpy version {}", target_name);
            BaseUpgradeResource.create_version(BaseUpgradeResource.last_version_id)
        }
      } catch {
        case _ =>
          Services.LOG.info("Creating new emtpy version {}", target_name);
          BaseUpgradeResource.create_version(BaseUpgradeResource.last_version_id)
      }
    }

    val ps = version.getProfiles
    ps.foreach(_.delete)

    profiles.keySet.foreach( (p) =>
      try {
        version.createProfile(p)
      } catch {
        case _ =>
          // ignore
      })

    profiles.asScala.foreach {
      case (profile: String, data: util.HashMap[String, Array[Byte]]) => {
        VersionResource.create_profile(version, data, profile)
      }
    }
    version.getId
  }

  //@GET @Path("default")
  //def default : VersionResource = new VersionResource(fabric_service.getDefaultVersion)

  @POST
  def create(options: CreateVersionDTO) = {

    val latestVersion = get.last.self

    val new_id = if (options.id == null || options.id == "" || options.id == "<unspecified>") {
      latestVersion.getSequence.next.getName
    } else {
      options.id
    }

    val derived_from: String = if (options.derived_from == null || options.derived_from == "" || options.derived_from == "none") {
      null
    } else {
      options.derived_from
    }

    val rc = Option(derived_from).flatMap(id => get.find(_.id == derived_from)) match {
      case Some(version) =>
        fabric_service.createVersion(version.self, new_id)
      case None =>
        fabric_service.createVersion(latestVersion, new_id)
    }
    new VersionResource(rc)
  }

  def delete_version(id: String) = get(id).self.delete

  @POST
  @Path("delete")
  def delete_versions(args: DeleteVersionsDTO) = args.ids.foreach(delete_version(_))

  @POST
  @Path("set_default")
  def set_default(args: SetDefaultVersionDTO) = {
    require(args.id != null, "Must specify a version name")
    val new_default = get(args.id).self
    fabric_service.setDefaultVersion(new_default)
  }

}


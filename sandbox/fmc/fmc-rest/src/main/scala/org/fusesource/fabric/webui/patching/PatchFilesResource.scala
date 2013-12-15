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

package io.fabric8.webui.patching

import javax.ws.rs._
import core.Context
import java.io._
import org.codehaus.jackson.annotate.JsonProperty
import com.sun.jersey.multipart.FormDataParam
import com.sun.jersey.core.header.FormDataContentDisposition
import org.apache.commons.io.IOUtils
import javax.ws.rs.core.Response.Status._
import javax.servlet.http.{HttpSession, HttpServletRequest}
import io.fabric8.webui.Services
import java.util.zip.ZipInputStream
import org.apache.commons.compress.archivers.zip.ZipFile
import javax.security.auth.Subject

/**
 *
 */
class ApplyFilePatchesDTO {
  @JsonProperty
  var target_version: String = _
}

class PatchFileResource(val parent: File, val name: String) extends BaseUpgradeResource {

  @JsonProperty
  def id = name

  @DELETE
  def delete = {
    val file = new File(parent, name)
    if (file.exists()) {
      file.delete
    }
  }

}

@Path("/patches/files")
class PatchFilesResource extends BaseUpgradeResource {

  @GET
  override def get = patch_files_resource

  @GET
  @Path("{id}")
  def get_file(@PathParam("id") id: String) = patch_files_resource.find(_.id.equals(id)).getOrElse {
    not_found
  }

  def patch_files_resource = patch_file_names.map(new PatchFileResource(get_or_create_patch_dir, _))

  def patch_files = {
    val patch_dir = get_or_create_patch_dir
    patch_dir.listFiles(new FilenameFilter {
      def accept(dir: File, name: String) = name.endsWith(".zip")
    })
  }

  def patch_file_names = patch_files.map(_.getName)

  def get_or_create_patch_dir = {
    val patch_dir = new File(Services.patch_dir)
    try {
      if (!patch_dir.exists()) {
        if (!patch_dir.mkdirs()) {
          throw new RuntimeException("Unable to create directory path : " + patch_dir.getAbsolutePath())
        }
      }
    } catch {
      case t: Throwable => respond(INTERNAL_SERVER_ERROR, "Failed to create patch directory : " + t.getMessage())
    }
    patch_dir
  }

  @Consumes(Array("multipart/form-data"))
  @Produces(Array("application/json"))
  @POST
  @Path("upload")
  def add(@FormDataParam("patch_file") patch_file: InputStream,
          @FormDataParam("patch_file") patch_file_detail: FormDataContentDisposition) = {

    Services.LOG.debug("Received file : {}", patch_file_detail)

    if (!patch_file_detail.getFileName.endsWith(".zip")) {
      respond(UNSUPPORTED_MEDIA_TYPE, "Uploaded patch file must be in a .zip format")
    }

    val patch_dir = get_or_create_patch_dir
    var temp = File.createTempFile(patch_file_detail.getFileName, ".upload")

    Services.LOG.debug("Created temp file : {}", temp.getAbsolutePath)

    try {
      val out = new FileOutputStream(temp)
      Services.LOG.debug("Copying output stream")
      IOUtils.copy(patch_file, out)
      out.flush
      out.close

      val target = new File(patch_dir, patch_file_detail.getFileName)
      temp.renameTo(target)
      temp = target;

      Services.LOG.debug("Moved temp file to : {}", temp.getAbsolutePath)

      //validate that the uploaded zip is actually a patch
      var repo_entries = 0;
      var descriptors = 0;
      val zip = new ZipFile(temp.getAbsolutePath);

      import collection.JavaConverters._

      zip.getEntries.asScala.foreach((x) => {
        if (!x.isDirectory) {
          if (x.getName.startsWith("repository/")) {
            repo_entries = repo_entries + 1
          } else if (x.getName.endsWith(".patch") && !x.getName.contains("/")) {
             descriptors = descriptors + 1
          }
        }
      })

      zip.close()

      Services.LOG.debug("Found " + repo_entries + " repository entries and " + descriptors + " patch descriptors");

      if (descriptors == 0) {
        throw new Exception("Provided file is not a valid patch file");
      }

      new PatchFileResource(patch_dir, temp.getName)
    } catch {
      case t: Throwable =>
        try {
          temp.delete()
        } catch {
          case _ =>
            Services.LOG.warn("Failed to delete file : " + temp.getAbsolutePath)
        }
        respond(INTERNAL_SERVER_ERROR, "Failure preparing patch file : " + t.getMessage())
    }
  }

  @DELETE
  @Path("{id}")
  def delete(@PathParam("id") id: String) = get_file(id).delete

  @POST
  @Path("go")
  def apply_patches(args: ApplyFilePatchesDTO, @Context request: HttpServletRequest) = {
    val session:HttpSession = request.getSession(false)
    if (session == null) {
      throw new WebApplicationException(UNAUTHORIZED)
    }

    val version = create_version(args.target_version)
    Services.LOG.info("Created version {}", version.getId)

    patch_files.foreach((x) => {
      try {
        Services.LOG.info("Applying patch {} to version {}", x.getName, version.getId)
        patch_service.applyFinePatch(version, x.toURI.toURL, Services.jmx_username(request), Services.jmx_password(request))
      } catch {
        case t: Throwable =>
          version.delete()
          Services.LOG.warn("Failed to apply patch " + x.getName, t)
          respond(INTERNAL_SERVER_ERROR, "Failed to apply patch " + x.getName + " due to " + t.getMessage);
      }
    })

    patch_files.foreach((x) => {
      try {
        Services.LOG.info("Deleting patch file {}", x.getName)
        x.delete
      } catch {
        case t: Throwable => Services.LOG.warn("Failed to delete patch file {} due to {}", x.getName, t.getMessage)
      }
    })

    version.getId
  }
}

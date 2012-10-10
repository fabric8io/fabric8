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

import org.fusesource.scalate.RenderContext

package

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
object Website {

  def uriStartsWith(s: String) = {
    var answer = false
    val rc = RenderContext()
    if (rc != null) {
      val uri = rc.requestUri
      if (uri != null) {
        answer = uri.startsWith(s)
      }
    }
    answer
  }

  def project_name 
    = if (uriStartsWith("/fabric")) "Fabric" else "Fuse"
  def project_slogan
    = if (uriStartsWith("/fabric")) 
      "Fuse Fabric: keeps you DRY from those stormy clouds!"
      else
        "Awesome open source integration"
        
  def project_id
  = if (uriStartsWith("/fabric")) "fabric" else "fuse"
  
  var project_jira_key= "FABRIC"
  val project_issue_url= "http://fusesource.com/issues/browse/FABRIC"
  val project_forums_url= "http://fuse.fusesource.org/community.html"
  val project_wiki_url= "http://wiki.github.com/fusesource/fuse/"
  val project_logo= "/images/project-logo.gif"
  val project_version= "7.0.1.fuse-084"
  val project_snapshot_version= "99-master-SNAPSHOT"
  val project_versions = List(
        project_version)  

  val github_page= "http://github.com/fusesource/fuse"
  val git_user_url= "git://github.com/fusesource/fuse.git"
  val git_commiter_url= "git@github.com:fusesource/fuse.git"

  // -------------------------------------------------------------------
  val project_svn_url= "http://fusesource.com/forge/svn/%s/trunk".format(project_id)
  val project_svn_branches_url= "http://fusesource.com/forge/svn/%s/branches".format(project_id)
  val project_svn_tags_url= "http://fusesource.com/forge/svn/%s/tags".format(project_id)
  val project_maven_groupId= "org.fusesource.%s".format(project_id)
  val project_maven_artifactId= "fuse-fabric"


}
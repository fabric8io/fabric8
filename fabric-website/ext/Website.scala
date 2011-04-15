import org.fusesource.scalate.RenderContext

/**
 * Copyright (C) 2009-2010 the original author or authors.
 * See the notice.md file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
object Website {

  val project_name= "Fabric"
  val project_slogan= "Fuse Fabric: keeps you DRY from those stormy clouds!"
  val project_id= "fabric"
  val project_jira_key= "FABRIC"
  val project_issue_url= "http://fusesource.com/issues/browse/FABRIC"
  val project_forums_url= "http://fabric.fusesource.org/community.html"
  val project_wiki_url= "http://wiki.github.com/fusesource/fabric/"
  val project_logo= "/images/project-logo.gif"
  val project_version= "1.0-SNAPSHOT"
  val project_snapshot_version= "1.0-SNAPSHOT"
  val project_versions = List(
        project_version,
        "1.0")  

  val github_page= "http://github.com/fusesource/fabric"
  val git_user_url= "git://github.com/fusesource/fabric.git"
  val git_commiter_url= "git@github.com:fusesource/fabric.git"

  // -------------------------------------------------------------------
  val project_svn_url= "http://fusesource.com/forge/svn/%s/trunk".format(project_id)
  val project_svn_branches_url= "http://fusesource.com/forge/svn/%s/branches".format(project_id)
  val project_svn_tags_url= "http://fusesource.com/forge/svn/%s/tags".format(project_id)
  val project_maven_groupId= "org.fusesource.%s".format(project_id)
  val project_maven_artifactId= "fabric-agent"

}
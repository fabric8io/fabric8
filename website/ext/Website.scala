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
import org.fusesource.scalate.RenderContext

package

object Website {

  val project_name= "fabric8.io"
  val project_slogan= "an open source integration platform"
  val project_id= "fabric8"
  val project_jira_key= "fabric8"
  val project_issue_url= "https://github.com/fabric8io/fabric8/issues?state=open"
  val project_forums_url= "http://fabric8.io/community/index.html"
  val project_wiki_url= "https://github.com/fabric8io/fabric8/wiki"
  val project_logo= "/images/project-logo.png"
  val project_version= "1.0"
  val project_snapshot_version= "1.1-SNAPSHOT"
  val project_versions = List(
        project_version
        )

  val github_page= "https://github.com/fabric8io/fabric8"
  val git_user_url= "git://github.com/fabric8io/fabric8.git"
  val git_commiter_url= "git@github.com:fabric8io/fabric8.git"
  val git_branch= "master"
  val git_edit_page_base = github_page+"/edit/"+git_branch+"/website/src"
  val disqus_shortname = project_id

  val scala_compat_tag = "2.9"

  // -------------------------------------------------------------------
  /*
  val project_maven_groupId= "io.hawt"
  val project_maven_artifactId= "fabric8-web"

  val app_dir = "../fabric8-web/src/main/webapp/app"
  */

}

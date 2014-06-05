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

  val project_name= "hawt.io"
  val project_slogan= "its a hawt console to help you stay cool!"
  val project_id= "hawtio"
  val project_jira_key= "hawtio"
  val project_issue_url= "https://github.com/hawtio/hawtio/issues?state=open"
  val project_forums_url= "http://hawt.io/community/index.html"
  val project_wiki_url= "https://github.com/hawtio/hawtio/wiki"
  val project_logo= "/images/project-logo.png"
  val project_version= "1.2.2"
  val project_snapshot_version= "1.3-SNAPSHOT"
  val project_versions = List(
        project_version
        )

  val github_page= "https://github.com/hawtio/hawtio"
  val git_user_url= "git://github.com/hawtio/hawtio.git"
  val git_commiter_url= "git@github.com:hawtio/hawtio.git"
  val git_branch= "master"
  val git_edit_page_base = github_page+"/edit/"+git_branch+"/website/src"
  val disqus_shortname = project_id

  val scala_compat_tag = "2.9"

  // -------------------------------------------------------------------
  val project_maven_groupId= "io.hawt"
  val project_maven_artifactId= "hawtio-web"

  val app_dir = "../hawtio-web/src/main/webapp/app"

}

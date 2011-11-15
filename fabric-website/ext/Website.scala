import org.fusesource.scalate.RenderContext

/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
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
  val project_version= "1.1-SNAPSHOT"
  val project_snapshot_version= "1.1-SNAPSHOT"
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
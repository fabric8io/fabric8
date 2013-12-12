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

import system.Principal
import java.util.Date
import java.text.SimpleDateFormat
import scala.concurrent.ops._
import javax.servlet.{ServletContext, ServletContextEvent, ServletContextListener}
import org.osgi.framework.{FrameworkUtil, Bundle}

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class ContextListener extends ServletContextListener {

  def contextInitialized(ctxEvent: ServletContextEvent) {

    Services.LOG.info("-- FMC Context Initialized --")
    Services.LOG.info("Patch upload directory at {}", Services.patch_dir)
    Services.LOG.info("Profile name is {}", Services.profile_name)
    Services.LOG.info("Create managed container : {}", Services.managed)
    Services.LOG.info("JAAS realm: {}", Services.realm);
    Services.LOG.info("JAAS authorized role: {}", Services.role);

  }

  def contextDestroyed(ctx: ServletContextEvent) {
    Services.LOG.info("-- FMC Context Destroyed --")
  }

}

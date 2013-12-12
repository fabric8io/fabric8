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

package io.fabric8.webui.jclouds

import org.jclouds.compute.ComputeService
import org.slf4j.LoggerFactory

/**
 *
 */
object ComputeServiceListener {
  val LOG = LoggerFactory.getLogger(classOf[ComputeServiceListener])
}

class ComputeServiceListener {

  import ComputeServiceListener._

  var compute_services = List[ComputeService]()

  def bind(cs: ComputeService) = {
    LOG.debug("Adding new compute service {}", cs)
    compute_services = compute_services :+ cs
  }

  def unbind(cs: ComputeService) = {
    LOG.debug("Removing compute service {}", cs)
    compute_services = compute_services diff List(cs)
  }

  def services = compute_services

  override def toString = "ComputeServiceListener : " + services.size + " services registered"

}

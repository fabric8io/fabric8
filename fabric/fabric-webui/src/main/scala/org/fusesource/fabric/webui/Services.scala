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
package org.fusesource.fabric.webui

import jclouds.ComputeServiceListener
import org.linkedin.zookeeper.client.{ZKClient, IZKClient}
import org.linkedin.util.clock.Timespan
import org.fusesource.fabric.service.{ContainerTemplate, FabricServiceImpl}
import java.util.concurrent.ConcurrentHashMap
import org.osgi.service.cm.{ConfigurationAdmin, Configuration}
import java.util.{Properties, Dictionary}
import org.osgi.framework.FrameworkUtil
import system.Principal
import org.slf4j.{Logger, LoggerFactory}
import java.lang.ExceptionInInitializerError
import org.fusesource.fabric.api.{ZooKeeperClusterService, Container, FabricService, Version, Profile}
import org.jclouds.karaf.core.ComputeProviderListener
import java.io.File

class Services {

  import Services._

  def setProfileName(name: String) = {
    _profile_name = name
  }

  def setResolver(resolver: String) = {
    _resolver = resolver
  }

  def setManaged(managed: Boolean) = {
    _managed = managed
  }

  def setTempDir(tempDir: String) = {
    _tempDir = tempDir
  }

  def setPatchDir(patchDir: String) = {
    _patchDir = patchDir

  }

  def setZooKeeper(service: IZKClient) = _zoo_keeper = service

  def setFabricService(service: FabricService) = _fabric_service = service

  def setZooKeeperClusterService(service: ZooKeeperClusterService) = _zoo_keeper_cluster_service = service

  def setConfigurationAdmin(service: ConfigurationAdmin): Unit = _config_admin = service

  def setComputeServiceListener(listener: ComputeServiceListener): Unit = _compute_service_listener = listener

  def setComputeProviderListener(listener: ComputeProviderListener): Unit = _compute_provider_listener = listener

}

object Services {

  val PID_FILTER = "(service.pid=%s*)"

  protected val jcloud_name_mapping = Map(
    "aws-ec2" -> "Amazon Elastic Compute Cloud (EC2)",
    "bluelock-vcloud-zone01" -> "Bluelock vCloud Zone 1",
    "cloudsigma-lvs" -> "CloudSigma LasVegas",
    "cloudsigma-zrh" -> "CloudSigma Zurich",
    "elastichosts-lon-b" -> "ElasticHosts London BlueSquare",
    "elastichosts-lon-p" -> "ElasticHosts London Peer 1",
    "elastichosts-sat-p" -> "ElasticHosts San Antonio Peer 1",
    "eucalyptus-partnercloud-ec2" -> "Eucalyptus Partner Cloud (EC2)",
    "go2cloud-jhb1" -> "Go2Cloud Johannesburg1",
    "gogrid" -> "GoGrid",
    "greenhousedata-element-vcloud" -> "Green House Data Element vCloud",
    "ninefold-compute" -> "Ninefold Compute",
    "openhosting-east1" -> "OpenHosting East1",
    "cloudservers-uk" -> "Rackspace Cloud Servers UK",
    "cloudservers-us" -> "Rackspace Cloud Servers US",
    "rimuhosting" -> "RimuHosting",
    "savvis-symphonyvpdc" -> "Savvis Symphony VPDC",
    "serverlove-z1-man" -> "Serverlove Manchester",
    "skalicloud-sdg-my" -> "SkaliCloud Malaysia",
    "slicehost" -> "Slicehost",
    "softlayer" -> "SoftLayer",
    "stratogen-vcloud-mycloud" -> "StratoGen VMware hosting",
    "trmk-ecloud" -> "Terremark Enterprise Cloud",
    "trmk-vcloudexpress" -> "Terremark vCloud Express"
  )

  val LOG: Logger = LoggerFactory.getLogger("org.fusesource.fabric.webui.Services")

  def debug = System.getProperty("org.fusesource.fabric.fabric-webui.debug", "false").asInstanceOf[String].toBoolean

  private lazy val _bundle = FrameworkUtil.getBundle(getClass)
  private lazy val _bundle_context = if (_bundle != null) _bundle.getBundleContext else null

  def bundle_context = _bundle_context

  var principal = new Principal

  def refresh = _bundle.update

  def get_service[T](clazz: Class[T]): T = {
    try {
      val reference = _bundle_context.getServiceReference(clazz.getName)
      if (reference == null) {
        throw new ExceptionInInitializerError(clazz.getName + " service reference is null")
      }

      val rc = clazz.cast(_bundle_context.getService(reference))
      if (rc == null) {
        throw new ExceptionInInitializerError(clazz.getName + " service is null")
      }
      rc
    } catch {
      case t: Throwable =>
        LOG.warn("Failed to get " + clazz.getName + " service due to", t)
        throw t
    }

  }

  def configs_by_factory_pid(pid: String): Array[Configuration] = {
    var rc = Array[Configuration]()
    try {
      rc = config_admin.listConfigurations(String.format(PID_FILTER, pid))
    } catch {
      case t: Throwable =>
    }
    rc
  }

  protected var _profile_name: String = _
  protected var _resolver: String = _

  protected var _managed: Boolean = _
  protected var _fabric_service: FabricService = _
  protected var _zoo_keeper_cluster_service: ZooKeeperClusterService = _
  protected var _config_admin: ConfigurationAdmin = _
  protected var _zoo_keeper: IZKClient = _
  protected var _compute_service_listener: ComputeServiceListener = _
  protected var _compute_provider_listener: ComputeProviderListener = _
  protected var _tempDir: String = _
  protected var _patchDir: String = _

  def profile_name = _profile_name

  def managed = _managed

  def resolver = _resolver

  def config_admin = _config_admin

  def fabric_service = _fabric_service

  def zk_cluster_service = _zoo_keeper_cluster_service

  def zoo_keeper = _zoo_keeper

  def compute_services = _compute_service_listener.services

  def compute_providers = _compute_provider_listener.getInstalledProviders

  def map_provider_name(key: String) = jcloud_name_mapping.get(key).getOrElse {
    ""
  }

  def patch_dir = temp_dir + File.separator + _patchDir

  def temp_dir = {
    if (_tempDir == null || "".equals(_tempDir)) {
      System.getProperty("java.io.tmpdir")
    } else {
      _tempDir
    }
  }

  case class AgentTemplateHolder(agent: Container) {
    lazy val template = new ContainerTemplate(agent, true, principal.username, principal.password)
  }

  val jmx_template = new ConcurrentHashMap[String, AgentTemplateHolder]()

  def agent_template(agent: Container) = {
    val tmp = AgentTemplateHolder(agent)
    var rc = jmx_template.putIfAbsent(agent.getId, tmp)
    if (rc == null) {
      rc = tmp
    }
    rc.template
  }

}

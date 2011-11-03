/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.activemq

import org.osgi.framework.BundleContext
import org.osgi.service.cm.ConfigurationException
import org.osgi.service.cm.ManagedServiceFactory
import org.slf4j.LoggerFactory
import reflect.BeanProperty
import org.linkedin.zookeeper.client.IZKClient
import java.util.{Properties, Dictionary}
import collection.mutable.HashMap
import java.util.concurrent.atomic.AtomicBoolean
import org.apache.activemq.broker.BrokerService
import org.springframework.core.io.Resource
import org.apache.activemq.spring.Utils
import org.apache.xbean.spring.context.ResourceXmlApplicationContext
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader
import java.lang.{ThreadLocal, Thread}
import java.beans.PropertyEditorManager
import java.net.URI
import org.apache.xbean.spring.context.impl.URIEditor
import org.springframework.beans.factory.FactoryBean

object ActiveMQServiceFactory {
  final val LOG= LoggerFactory.getLogger(classOf[ActiveMQServiceFactory])
  final val CONFIG_PROPERTIES = new ThreadLocal[Properties]()

  PropertyEditorManager.registerEditor(classOf[URI], classOf[URIEditor])

  def debug(str: String, args: AnyRef*) = if (LOG.isDebugEnabled) {
    LOG.debug(String.format(str, args))
  }

  def warn(str: String, args: AnyRef*) = if (LOG.isDebugEnabled) {
    LOG.warn(String.format(str, args))
  }

  implicit def toProperties(properties: Dictionary[_, _]) = {
    var props: Properties = new Properties
    var ek = properties.keys
    while (ek.hasMoreElements) {
      var key = ek.nextElement
      var value = properties.get(key)
      props.put(key.toString, if (value != null) value.toString else "")
    }
    props
  }

  def arg_error[T](msg:String):T = {
    throw new IllegalArgumentException(msg)
  }

  def createBroker(uri: String, properties:Properties) = {
    CONFIG_PROPERTIES.set(properties)
    try {
      Thread.currentThread.setContextClassLoader(classOf[BrokerService].getClassLoader)
      var resource: Resource = Utils.resourceFromString(uri)
      val ctx = new ResourceXmlApplicationContext((resource)) {
        protected override def initBeanDefinitionReader(reader: XmlBeanDefinitionReader): Unit = {
          reader.setValidating(false)
        }
      }
      var names: Array[String] = ctx.getBeanNamesForType(classOf[BrokerService])
      val broker = names.flatMap{ name=> Option(ctx.getBean(name).asInstanceOf[BrokerService]) }.headOption.getOrElse(arg_error("Configuration did not contain a BrokerService"))
      (ctx, broker)
    } finally {
      CONFIG_PROPERTIES.remove()
    }
  }

}

class ConfigurationProperties extends FactoryBean[Properties] {
  def getObject = new Properties(ActiveMQServiceFactory.CONFIG_PROPERTIES.get())
  def getObjectType = classOf[Properties]
  def isSingleton = false
}

class ActiveMQServiceFactory extends ManagedServiceFactory {

  import ActiveMQServiceFactory._

  @BeanProperty
  var bundleContext: BundleContext = null
  @BeanProperty
  var zooKeeper: IZKClient = null

  case class ClusteredConfiguration(properties:Properties) {

    val name = Option(properties.getProperty("name")).getOrElse(arg_error("name property must be set"))
    val config = Option(properties.getProperty("config")).getOrElse(arg_error("config property must be set"))
    val group = Option(properties.getProperty("group")).getOrElse("default")
    val connectors = Option(properties.getProperty("connectors")).getOrElse("").split("""\s""")

    val discoveryAgent = new FabricDiscoveryAgent
    discoveryAgent.setId(name)
    discoveryAgent.setGroupName(group)
    discoveryAgent.setZkClient(zooKeeper)
    val started = new AtomicBoolean

    @volatile
    var start_thread:Thread = _
    @volatile
    var server:(ResourceXmlApplicationContext, BrokerService) = _

    def start = {
      def trystartup:Unit = {
        start_thread = new Thread("Startup for ActiveMQ Broker: "+name) {
          override def run() {
            var start_failure:Throwable = null
            try {
              discoveryAgent.start()

              // Don't start up until we acquire the master node lock.
              while( !discoveryAgent.singleton.isMaster ) {
                Thread.sleep(500)
              }

              // ok boot up the server..
              server = createBroker(config, properties)
              server._2.start()

              // Update the advertised endpoint URIs that clients can use.
              discoveryAgent.setServices( connectors.flatMap { name=>
                val connector = server._2.getConnectorByName(name)
                if ( connector==null ) {
                  warn("ActiveMQ broker '%s' does not have a connector called '%s'", name, name)
                  None
                } else {
                  Some(connector.getPublishableConnectString)
                }
              })

            } catch {
              case e:Throwable =>
                // Try again in a little bit.
                e.printStackTrace()
                Thread.sleep(1000*10);
                start_failure = e
            } finally {
              if(started.get && start_failure!=null){
                trystartup
              } else {
                start_thread = null
              }
            }
          }
        }
        start_thread.start()
      }
      if(started.compareAndSet(false, true)) {
        trystartup
      }
      this
    }
    
    def stop = {
      if(started.compareAndSet(true, false)) {
        var t = start_thread // working with a volatile
        while(t!=null) {
          t.interrupt()
          t.join()
          t = start_thread // when the start up thread gives up trying to start this gets set to null.
        }

        val s = server // working with a volatile
        if( s!=null ) {
          try {
            s._2.stop()
          } catch {
            case e:Throwable => e.printStackTrace()
          }
          try {
            s._1.close()
          } catch {
            case e:Throwable => e.printStackTrace()
          }
          try {
            discoveryAgent.stop()
          } catch {
            case e:Throwable => e.printStackTrace()
          }
          server = null
        }
      }
      this
    }
    
  }

  ////////////////////////////////////////////////////////////////////////////
  // Maintain a registry of configuration based on ManagedServiceFactory events.
  ////////////////////////////////////////////////////////////////////////////
  val configurations = new HashMap[String, ClusteredConfiguration]
  def updated(pid: String, properties: Dictionary[_, _]): Unit = {
    try {
      deleted(pid)
      configurations.put(pid, ClusteredConfiguration(properties).start)
    } catch {
      case e: Exception => throw new ConfigurationException(null, "Unable to parse ActiveMQ configuration: " + e.getMessage).initCause(e).asInstanceOf[ConfigurationException]
    }
  }
  def deleted(pid: String): Unit = {
    configurations.remove(pid).foreach(_.stop)
  }
  def destroy: Unit = configurations.keys.toArray.foreach(deleted(_))

  def getName: String = {
    return "ActiveMQ Server Controller"
  }

}
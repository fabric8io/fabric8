package org.fusesource.fabric.monitor.plugins

import jmx.{JmxConstants, MBeanAttributeKeyPollDTO, JmxDataSourceRegistry}
import org.fusesource.fabric.monitor.api.{PollDTO, DataSourceDTO, MonitoredSetDTO}
import collection.JavaConverters._

/**
 * A helper class for building a monitor set
 */
abstract class MonitorSetBuilder(name: String) {
  var set: MonitoredSetDTO = _
  val jmxFactory = new JmxDataSourceRegistry()

  def apply(): MonitoredSetDTO = {
    set = new MonitoredSetDTO(name)
    configure
    set
  }

  def configure: Unit

  def dataSource(poll: PollDTO, id: String, name: String = null, description: String = null, kind: String = "guage", heartbeat: String = "1s", min: Double = Double.NaN, max: Double = Double.NaN) = {
    var n = if (name == null) id else name
    var d = if (description == null) n else description
    val ds = new DataSourceDTO(id, n, d, kind, heartbeat, min, max, poll)
    addDataSource(ds)
    ds
  }

  def jmxDataSources(ids: String*): List[DataSourceDTO] = {
    var answer = List[DataSourceDTO]()
    for (id <- ids) {
      val paths = id.split(JmxConstants.SEPARATOR)
     val ds: Option[DataSourceDTO] = if (paths.length < 2) {
        throw new IllegalArgumentException("JMX IDs must have at least 2 paths separated by " + JmxConstants.SEPARATOR)
      } else if (paths.length < 3) {
        jmxDataSource(paths(0), paths(1))
      } else {
        jmxDataSource(paths(0), paths(1), paths(2))
      }
      if (ds.isDefined) {
        answer :+ ds.get
      }
    }
    answer
  }

  def jmxDataSource(objectName: String, attributeName: String, key: String): Option[DataSourceDTO] = {
    jmxFactory.createDataSource(objectName, attributeName) match {
      case Some(ds) =>
        val optKeyDS = ds.children.asScala.find(_.poll match {
          case mak: MBeanAttributeKeyPollDTO => key == mak.key
          case _ => false
        })
        addDataSource(optKeyDS, "No key " + key + " in MBean " + objectName + " attribute " + attributeName)
      case _ => None
    }
  }

  def jmxDataSource(objectName: String, attributeName: String): Option[DataSourceDTO] = {
    val answer = jmxFactory.createDataSource(objectName, attributeName)
    addDataSource(answer, "No attribute " + attributeName + " in MBean " + objectName)
  }

  def processPoll(name: String) = {
    // TODO!
    new ProcessPollDTO()
  }

  def addDataSource(ds: DataSourceDTO): Unit = {
    set.data_sources.add(ds)
  }

  def addDataSource(answer: Option[DataSourceDTO], message: => String): Option[DataSourceDTO] = {
    answer match {
      case Some(ds) => addDataSource(ds)
      case _ =>
      // TODO
        println(message)
    }
    answer
  }


}
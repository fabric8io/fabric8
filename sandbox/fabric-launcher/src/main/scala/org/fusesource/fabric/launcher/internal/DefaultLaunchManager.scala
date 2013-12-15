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

package io.fabric8.launcher.internal

import org.fusesource.hawtdispatch._
import java.util.concurrent.TimeUnit
import collection.mutable.ListBuffer
import collection.mutable.HashMap
import org.hyperic.sigar.Sigar
import java.io.File
import java.lang.String
import FileSupport._
import ProcessSupport._
import io.fabric8.launcher.api.{ServiceStatusDTO, LaunchManager, ServiceDTO}
import io.fabric8.monitor.internal.{DefaultMonitor, ClassFinder}
import io.fabric8.monitor.api._
import io.fabric8.monitor.plugins.{ProcessPollDTO, ProcessPollerFactory}
import io.fabric8.monitor.api.{PollerFactory, MonitoredSetDTO, ArchiveDTO}

object DefaultLaunchManager {

  val DATA_POLLER_FACTORY_RESOURCE = "META-INF/services/io.fabric8.monitor/poller-factory.index"

}

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class DefaultLaunchManager(stats_dir:File) extends LaunchManager {
  import DefaultLaunchManager._

  val sigar = new Sigar

  // Use an embedded monitor to extract the status of the
  // launched services..

  stats_dir.mkdirs
  val monitor = new DefaultMonitor(stats_dir.getCanonicalPath+"/");
  {
    val finder = new ClassFinder(DATA_POLLER_FACTORY_RESOURCE, classOf[PollerFactory])
    monitor.poller_factories = finder.singletons
  }
  val dispatch_queue = createQueue("LaunchManager")

  val periodic_check = dispatch_queue.repeatAfter(1, TimeUnit.SECONDS) {
    current_services.values.foreach(_.check)
  }
  
  trait StatusCheck {
    def pid:Option[Long]
    def is_running:Boolean
  }

  def is_pid_running(pid:Long) = {
    try {
      sigar.getProcList.contains(pid)
    } catch {
      case e:Throwable =>
        e.printStackTrace
        false
    }
  }

  case class PidFileStatusCheck(pid_file:File) extends StatusCheck {
    def pid = {
      if( pid_file.exists ) {
        try {
          Some(new String(pid_file.read_bytes).trim.toLong)
        } catch {
          case _ => None
        }
      } else {
        None
      }
    }

    def is_running:Boolean = {
      pid.map{ pid=>
        val rc = is_pid_running(pid)
        if( !rc ) {
          // delete the pid file if the process is no longer running.
          pid_file.delete
        }
        rc
      }.getOrElse(false)
    }

  }

  trait State {
    val at:Long = System.currentTimeMillis
    def age = System.currentTimeMillis-at
    def error:String=null
    def check:Unit
  }

  case class Service (
    id:String,
    start_on_boot:Boolean,
    start_command:List[String],
    stop_command:List[String],
    status_check:StatusCheck
  ) {


    val monitor_config = {
      val rc = new MonitoredSetDTO
      rc.name = id
      rc.step = "1s"

      def archive(window:String, step:String="1s", consolidation:String="AVERAGE") = {
        val rc = new ArchiveDTO
        rc.consolidation = consolidation
        rc.step = step
        rc.window = window
        rc
      }

      rc.archives.add(archive("5m"))
      rc.archives.add(archive("24h", "1m"))
      rc.archives.add(archive("30d", "1h"))
      rc.archives.add(archive("1y", "1d"))

      // Lets gather all the stats that the process poller can get us..
      ProcessPollerFactory.discover(null).foreach( rc.data_sources.add(_) )
      rc
    }

    def update_monitoring_pid(pid:java.lang.Long) = {
      import collection.JavaConversions._
      monitor_config.data_sources.foreach { source =>
        source.poll match {
          case x:ProcessPollDTO => x.pid = pid
          case _ =>
        }
      }
    }


    override def toString: String = "Service(%s, %s)".format(id, state)

    val dispatch_queue = createQueue("service: "+id)

    val disable_listeners = ListBuffer[() => Unit]()

    // make these volatile so that a monitoring thread can
    // directly access the current state.

    @volatile
    var enabled = start_on_boot
    @volatile
    var state:State = _

    def init = {
      state = if( status_check.is_running ) {
        Running()
      } else {
        Stopped()
      }
      dispatch_queue {
        state.check
      }
    }

    def updating(prev:Service) = {
      state = Updating(prev)
      prev.dispatch_queue {

        val start_state = if(start_on_boot == prev.start_on_boot) {
          prev.enabled
        } else {
          start_on_boot
        }

        dispatch_queue {
          enabled = start_state
          prev.disable {
            dispatch_queue {
              state = Stopped()
              state.check
            }
          }
        }
      }
    }

    def enable:Unit = dispatch_queue {
      enabled = true
      state.check
    }

    def disable(on_disabled: =>Unit ):Unit = dispatch_queue {
      disable_listeners.append( on_disabled _ )
      enabled  = false
      state.check
    }

    def check = dispatch_queue {
      state.check
    }

    private def start(attempt:Int):State = {
      try {
        System.out.println("starting service with: "+start_command.mkString(" "))
        val pb = new ProcessBuilder().command(start_command: _*)
        val process = pb.start(System.out, System.err)
        Starting(attempt, process)
      } catch {
        case e:Throwable =>
          object DelayedStart extends Function0[State] {
            def apply = start(attempt+1)
          }
          Delay(1000, DelayedStart, e.toString)
      }
    }

    private def stop:State = {
      if( status_check.is_running ) {
        val pid = status_check.pid
        try {
          System.out.println("stopping service with: "+start_command.mkString(" "))
          val pb = new ProcessBuilder().command(stop_command: _*)
          val process = pb.start(System.out, System.err)
          Stopping(process, pid)
        } catch {
          case e:Throwable =>

            if( pid.isDefined ) {
              Killing(pid.get)
            } else {
              // the stop command is our only option so try again
              // after a delay
              object DelayedStop extends Function0[State] {
                def apply = stop
              }
              Delay(1000, DelayedStop, e.toString)
            }
        }
      } else {
        Stopped()
      }
    }

    //////////////////////////////////////////////////////////////////
    //
    // State Machine Impl
    //
    //////////////////////////////////////////////////////////////////
    case class Stopped(override val error:String=null) extends State {
      update_monitoring_pid(null)
      def check = {
        // trigger any disabled listeners now..
        disable_listeners.foreach(_())
        disable_listeners.clear

        if( enabled ) {
          state = start(0)
          state.check
        }
      }
    }

    case class Starting(attempt:Int, process:Process) extends State {
      process.on_exit { code =>
        dispatch_queue {
          state = if( status_check.is_running ) {
            Running()
          } else {
            object DelayedStart extends Function0[State] {
              def apply = start(attempt+1)
            }
            Delay(1000, DelayedStart, "pid is not running")
          }
          state.check
        }
      }

      def check = {
        // TODO: perhaps enforce time limit on how long the start command can run?
      }
    }


    case class Running() extends State {

      status_check.pid.foreach(update_monitoring_pid(_))

      def check = {
        if( enabled ) {
          if( status_check.is_running ) {
            // we are at the desired state...
          } else {
            update_monitoring_pid(null)
            state = start(0)
            state.check
          }
        } else {
          state = stop
          state.check
        }
      }


    }

    case class Delay(delay:Long, then:()=>State, override val error:String=null) extends State {
      val enabled_at_start = enabled

      dispatch_queue.after(delay, TimeUnit.MILLISECONDS) {
        state.check
      }
      def check = {
        // if the enabled state changes or the delay duration
        // passes we can do the transition
        if( enabled_at_start!=enabled || age >= delay ) {
          state = then()
          state.check
        }
      }
    }

    case class Stopping(process:Process, pid:Option[Long]) extends State {
      process.on_exit { code =>
        dispatch_queue {
          state = if( enabled ) {
            // looks like we don't want to stop anymore..
            if( status_check.is_running ) {
              Running()
            } else {
              Stopped()
            }
          } else {
            // stop command might fail.. follow up with a hard kill.
            if( status_check.is_running ) {
              if(pid.isDefined) {
                println("Stop command failed to stop the process.  Killing")
                Killing(pid.get)
              } else {
                object DelayedStop extends Function0[State] {
                  def apply = stop
                }
                Delay(1000, DelayedStop, "Service was still running after stop command was run.")
              }
            } else {
              Stopped()
            }
          }
          state.check
        }
      }

      def check = {
        // this state ends once the stop command finishes executing..
        // TODO: we could implement a timeout here
      }
    }

    case class Killing(pid:Long) extends State {

      def check = {
        if( is_pid_running(pid) ) {
          try {
            System.out.println("Sending kill signal to: "+pid)
            sigar.kill(pid, 9)
          } catch {
            case _ =>
          }
          if( !is_pid_running(pid) ) {
            state = Stopped()
            state.check
          }
        } else {
          state = Stopped()
          state.check
        }
      }
    }

    case class Updating(previous:Service) extends State {
      def check = {
        // we can't don't state until the previous
        // service instance stops.
      }
    }

  }

  var current_services = HashMap[String, Service]()

  def status:Future[Seq[ServiceStatusDTO]] = dispatch_queue future {
    current_services.values.map { service =>

      val status = new ServiceStatusDTO
      status.id = service.id
      status.enabled = service.enabled
      status.pid = service.status_check.pid.map(new java.lang.Long(_)).getOrElse(null)

      val state = service.state
      status.state = state.toString
      status.state_age = state.age
      status.monitors = service.monitor_config
      status

    }.toSeq
  }

  def configure( value:Traversable[ServiceDTO] ) = dispatch_queue {

    import collection.JavaConversions._

    def file_separator(command:List[String]) = {
      command match {
        case first :: rest =>
          def command_args = FileSupport.fix_file_separator(first) :: rest
	      if( is_os_windows ) {
	      	"cmd" :: "/c"  :: command_args
	      } else {
	      	command_args
	      } 
	      
        case _ => command
      }
    }

    val next_services = Map[String, Service]( value.map { dto=>
      val enabled = dto.enabled==null || dto.enabled.booleanValue
      val start_command = file_separator(collectionAsScalaIterable(dto.start).toList)
      val end_command = file_separator(collectionAsScalaIterable(dto.stop).toList)
      val status_check=new PidFileStatusCheck(new File(dto.pid_file))
      dto.id -> Service(dto.id, enabled, start_command, end_command, status_check)
    }.toSeq : _*)


    // Figure out which services are being added, removed, or updated.
    val existing_keys = current_services.keys.toSet
    val next_service_keys = next_services.keys.toSet
    val updating = existing_keys.intersect(next_service_keys)
    val adding = next_service_keys -- updating
    val removing = existing_keys -- next_service_keys

    adding.foreach { id =>
      val next = next_services.get(id).get
      next.init
      current_services += id -> next
    }

    updating.foreach { id =>
      val next = next_services.get(id).get
      val prev = current_services.get(id).get

      // did the service configuration change?
      if( next != prev ) {
        next.updating(prev)
        current_services.put(id, next)
      }
    }

    removing.foreach{ id =>
      val prev = current_services.get(id).get
      prev.disable { dispatch_queue {
          if( current_services.get(id) == Some(prev) ) {
            current_services -= id
          }
        }
      }
    }

    val monitor_config = current_services.values.map(_.monitor_config).toList
    monitor.configure(monitor_config)

  }

  def close = {
    periodic_check.close
    sigar.close
  }

}

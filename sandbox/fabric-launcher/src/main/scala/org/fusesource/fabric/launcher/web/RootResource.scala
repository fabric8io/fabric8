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
package io.fabric8.launcher.web

import java.lang.String
import javax.ws.rs._
import com.sun.jersey.api.view.ImplicitProduces
import core.{UriInfo, Response, Context}
import Response._
import java.net.URI
import Response.Status._
import java.util.concurrent.TimeUnit
import org.fusesource.scalate.{NoValueSetException, RenderContext}
import java.awt.Color
import org.rrd4j.graph.{RrdGraphDef, RrdGraph}
import org.rrd4j.core.Util
import collection.JavaConversions._
import org.rrd4j.ConsolFun._
import io.fabric8.launcher.internal.DefaultLaunchManager
import io.fabric8.monitor.api.DataSourceDTO

/**
 * Defines the default representations to be used on resources
 */
@ImplicitProduces(Array("text/html;qs=5"))
@Produces(Array("application/json", "application/xml","text/xml"))
abstract class Resource(private val parent:Resource=null)  {

  @Context
  var uri_info:UriInfo = null

  if( parent!=null ) {
    this.uri_info = parent.uri_info
  }

  def result[T](value:Status, message:Any=null):T = {
    val response = Response.status(value)
    if( message!=null ) {
      response.entity(message)
    }
    throw new WebApplicationException(response.build)
  }

  def result[T](uri:URI):T = {
    throw new WebApplicationException(seeOther(uri).build)
  }

  def path(value:Any) = uri_info.getAbsolutePathBuilder().path(value.toString).build()

  def strip_resolve(value:String) = {
    new URI(uri_info.getAbsolutePath.resolve(value).toString.stripSuffix("/"))
  }

}

object ViewHelper {

  val KB: Long = 1024
  val MB: Long = KB * 1024
  val GB: Long = MB * 1024
  val TB: Long = GB * 1024

  val SECONDS: Long = TimeUnit.SECONDS.toMillis(1)
  val MINUTES: Long = TimeUnit.MINUTES.toMillis(1)
  val HOURS: Long = TimeUnit.HOURS.toMillis(1)
  val DAYS: Long = TimeUnit.DAYS.toMillis(1)
  val YEARS: Long = DAYS * 365


  def memory(value:Int):String = memory(value.toLong)
  def memory(value:Long):String = {

    if( value < KB ) {
      "%d bytes".format(value)
    } else if( value < MB ) {
       "%,.2f kb".format(value.toFloat/KB)
    } else if( value < GB ) {
      "%,.3f mb".format(value.toFloat/MB)
    } else if( value < TB ) {
      "%,.4f gb".format(value.toDouble/GB)
    } else {
      "%,.5f tb".format(value.toDouble/TB)
    }
  }

  def uptime(value:Long):String = {
    def friendly(duration:Long):String = {
      if( duration < SECONDS ) {
        "%d ms".format(duration)
      } else if (duration < MINUTES) {
        "%d seconds".format(duration / SECONDS)
      } else if (duration < HOURS) {
        "%d minutes".format(duration / MINUTES)
      } else if (duration < DAYS) {
        println("<")
        "%d hours %s".format(duration / HOURS, friendly(duration%HOURS))
      } else if (duration < YEARS) {
        "%d days %s".format(duration / DAYS, friendly(duration%DAYS))
      } else {
        "%,d years %s".format(duration / YEARS, friendly(duration%YEARS))
      }
    }
    friendly(System.currentTimeMillis - value)
  }

  def link_to_graph(service:String, kind:String):String = {
    "/graph/"+service+"/"+kind+".png"
  }

}
class ViewHelper {
  lazy val uri_info = {
    try {
      RenderContext().attribute[UriInfo]("uri_info")
    } catch {
      case x:NoValueSetException =>
        RenderContext().attribute[Resource]("it").uri_info
    }
  }

  def path(value:Any) = {
    uri_info.getAbsolutePathBuilder().path(value.toString).build()
  }

  def strip_resolve(value:String) = {
    uri_info.getAbsolutePath.resolve(value).toString.stripSuffix("/")
  }

}

object RootResource {
  @volatile
  var launch_manager:Option[DefaultLaunchManager] = None
}

/**
 * Manages a collection of broker resources.
 */
@Path("/")
class RootResource extends Resource {

  lazy val launch_manager = RootResource.launch_manager.getOrElse(result(NOT_FOUND))

  @GET
  @Produces(Array("text/html"))
  def get = RootResource.launch_manager.map(x=> this).getOrElse(result(NOT_FOUND))

  @GET
  @Path("graph/{service}/memory.png")
  @Produces(Array("text/html"))
  def memory(@PathParam("service") service:String):Response = {

    val ss = launch_manager.status().find(_.id == service).getOrElse(result(NOT_FOUND))
    val rrd_file = launch_manager.monitor.path_to_rrd_file(service)

    val now = Util.getTimestamp
    // then create a graph definition
    val gd = new RrdGraphDef()

    // Date lastUpdate = node.getProbe().getLastUpdate();

		gd.comment("\\l");
		gd.comment("\\l");
		gd.comment("Last update: something\\L");
		gd.comment("Unit type: bytes\\r");

    gd.setImageFormat("png")
    gd.setWidth(300)
    gd.setHeight(150)
    gd.setStartTime(now - 60)
    gd.setEndTime(now)

    gd.setTitle("Memory Usage")
    gd.setVerticalLabel("bytes")
    gd.setAntiAliasing(true)

    def add(id:String, color:Color, label:String) = {
      ss.monitors.data_sources.find(_.id == id).foreach { x=>
        gd.datasource(id, rrd_file, id, AVERAGE)
        gd.line(id, color, label)
      }
    }


//mem-major-faults
//mem-minor-faults
    add("mem-resident", Color.GREEN, "Memory Resident")
    add("mem-size", Color.BLUE, "Memory Size")

//    gDef.datasource("median", "inbytes,outbytes,+,2,/")

    val image = new RrdGraph(gd).getRrdGraphInfo().getBytes

    println("res"+image.length)
    Response.ok(image, "image/png").build()
  }

  @GET
  @Path("graph/{service}/cpu.png")
  @Produces(Array("text/html"))
  def cpu(@PathParam("service") service:String):Response = {

    val ss = launch_manager.status().find(_.id == service).getOrElse(result(NOT_FOUND))
    val rrd_file = launch_manager.monitor.path_to_rrd_file(service)

    val now = Util.getTimestamp
    // then create a graph definition
    val gDef = new RrdGraphDef()
    gDef.setImageFormat("png")
    gDef.setWidth(300)
    gDef.setHeight(150)
    gDef.setStartTime(now - 60)
    gDef.setEndTime(now)

    gDef.setTitle("CPU Usage")
    gDef.setVerticalLabel("%")

    gDef.setMaxValue(100);
    gDef.setMinValue(0);
    gDef.setRigid(true);

//    gDef.gprint(ds, LAST, "%6.2f%s");
//    gDef.gprint(ds, AVERAGE, "%8.2f%s");
//    gDef.gprint(ds, MIN, "%8.2f%s");
//    gDef.gprint(ds, MAX, "%8.2f%s");

    def add(id:String, color:Color, label:String) = {
      ss.monitors.data_sources.find(_.id == id).foreach { x=>
        gDef.datasource(id, rrd_file, id, AVERAGE)
        gDef.line(id, color, label)
      }
    }

//cpu-last"
//cpu-start"

    add("cpu-percent", Color.BLUE, "CPU Percent")
    add("cpu-sys", Color.RED, "CPU System")
    add("cpu-total", Color.GREEN, "CPU Total")


//    gDef.datasource("median", "inbytes,outbytes,+,2,/")

    val image = new RrdGraph(gDef).getRrdGraphInfo().getBytes

    println("res"+image.length)
    Response.ok(image, "image/png").build()
  }

}




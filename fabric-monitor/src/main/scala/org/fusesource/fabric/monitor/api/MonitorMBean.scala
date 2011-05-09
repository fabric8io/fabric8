package org.fusesource.fabric.monitor.api

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
trait MonitorMBean {
  def fetch( fetch:String ):String;
}
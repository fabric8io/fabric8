package org.fusesource.insight.maven.aether

import org.apache.maven.wagon.Wagon
import org.apache.maven.wagon.providers.file.FileWagon
import org.sonatype.aether.connector.wagon.WagonProvider
import org.apache.maven.wagon.providers.http.LightweightHttpWagon

class ManualWagonProvider extends WagonProvider {

  def lookup(roleHint: String) = roleHint match {
    case "file" => new FileWagon()
    case "http" => new LightweightHttpWagon()
    case _ => null
  }

  def release(wagon: Wagon) = {
  }
}
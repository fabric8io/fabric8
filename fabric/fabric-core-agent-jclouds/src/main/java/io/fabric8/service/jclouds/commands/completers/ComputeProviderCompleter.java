/*
 * Copyright (C) FuseSource, Inc.
 *   http://fusesource.com
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package io.fabric8.service.jclouds.commands.completers;

import java.util.List;
import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;
import org.jclouds.compute.ComputeService;

public class ComputeProviderCompleter implements Completer {

  protected final StringsCompleter delegate = new StringsCompleter();
  private List<ComputeService> computeServices;

  @Override
  public int complete(String buffer, int cursor, List<String> candidates) {
    delegate.getStrings().clear();
    if (computeServices != null && !computeServices.isEmpty()) {
      for(ComputeService computeService:computeServices) {
        delegate.getStrings().add(computeService.getContext().unwrap().getId());
      }
    }
    return delegate.complete(buffer, cursor, candidates);
  }

  public List<ComputeService> getComputeServices() {
    return computeServices;
  }

  public void setComputeServices(List<ComputeService> computeServices) {
    this.computeServices = computeServices;
  }
}
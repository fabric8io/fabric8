/**
 *  Copyright 2005-2015 Red Hat, Inc.
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
package io.fabric8.forge.rest.ui;

import org.jboss.forge.addon.ui.UIRuntime;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.input.UIPrompt;
import org.jboss.forge.addon.ui.progress.DefaultUIProgressMonitor;
import org.jboss.forge.addon.ui.progress.UIProgressMonitor;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 
 * @author <a href="ggastald@redhat.com">George Gastaldi</a>
 */
public class RestUIRuntime implements UIRuntime
{
   private final Collection<String> inputQueue;

   public RestUIRuntime(List<String> inputQueue)
   {
      super();
      this.inputQueue = inputQueue;
   }

   public RestUIRuntime()
   {
      this.inputQueue = Collections.emptyList();
   }

   @Override
   public UIProgressMonitor createProgressMonitor(UIContext context)
   {
      return new DefaultUIProgressMonitor();
   }

   @Override
   public UIPrompt createPrompt(UIContext context)
   {
      return new RestUIPrompt(inputQueue);
   }

}

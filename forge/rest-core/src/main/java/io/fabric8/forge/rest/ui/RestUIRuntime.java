/**
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
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

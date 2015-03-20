/**
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.fabric8.forge.rest.ui;

import org.jboss.forge.addon.ui.input.UIPrompt;

import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;

/**
 * 
 * @author <a href="ggastald@redhat.com">George Gastaldi</a>
 */
public class RestUIPrompt implements UIPrompt
{
   private final Deque<String> input = new LinkedList<String>();

   public RestUIPrompt()
   {
   }

   public RestUIPrompt(Collection<String> inputs)
   {
      input.addAll(inputs);
   }

   @Override
   public String prompt(String message)
   {
      if (input.isEmpty())
         throw new InputRequiredException(message);
      return input.pop();
   }

   @Override
   public String promptSecret(String message)
   {
      if (input.isEmpty())
         throw new InputRequiredException(message);
      return input.pop();
   }

   @Override
   public boolean promptBoolean(String message)
   {
      return promptBoolean(message, false);
   }

   @Override
   public boolean promptBoolean(String message, boolean defaultValue)
   {
      if (input.isEmpty())
         return defaultValue;
      return "Y".equalsIgnoreCase(input.pop());
   }
}

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

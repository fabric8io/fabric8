/**
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.fabric8.forge.rest.ui;

/**
 * 
 * @author <a href="ggastald@redhat.com">George Gastaldi</a>
 */
public class InputRequiredException extends RuntimeException
{
   private static final long serialVersionUID = 1L;

   public InputRequiredException(String message)
   {
      super(message);
   }
}

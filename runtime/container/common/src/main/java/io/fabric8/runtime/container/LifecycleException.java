package io.fabric8.runtime.container;

/**
 * LifecycleException
 *
 * @since 26-Feb-2014
 */
public class LifecycleException extends Exception
{
   private static final long serialVersionUID = 1L;

   public LifecycleException(String message)
   {
      super(message);
   }

   public LifecycleException(String message, Throwable cause)
   {
      super(message, cause);
   }

}

package io.fabric8.monitor.internal

/**
 * Helper methods for working with numbers
 */
object Numbers {

  def toNumber(value: Any, message: => String): Double = {
    value match {
      case n: Number => n.doubleValue
      case null => throw new NullPointerException(message + " is not a number")
      case x: AnyRef =>
        x.toString.toDouble
      //case v: AnyRef => throw new ValueNotNumberException(message, v)
    }
  }
}

class ValueNotNumberException(message: String, val value: AnyRef)
        extends IllegalArgumentException(message + " is not a number " + value + (if (value != null) {
          " of type " + value.getClass.getName
        } else " it was null")) {

}

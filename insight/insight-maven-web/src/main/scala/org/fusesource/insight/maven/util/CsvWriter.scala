package org.fusesource.insight.maven.util

import java.io.Writer


class CsvWriter(out: Writer) {
  var columnSeparator = ","
  var rowSeparator = "\n"

  private var column = 0

  def println(values: Any*): Unit = {
    for (v <- values) {
      print(v)
    }
    println
  }

  def print(v: Any): Unit = {
    if (column != 0) out.write(columnSeparator)
    v match {
      case n: Number => out.write("" + n)
      case _ => out.write("\"" + v + "\"")
    }
    column += 1
  }

  def println: Unit = {
    out.write(rowSeparator)
    column = 0
  }
}
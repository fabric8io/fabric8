/*
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.camel.tooling.util

class Languages {
  val languages = List(
    // No longer supported by Camel
    // Language("beanshell", "BeanShell", "BeanShell expression"),
    Language("constant", "Constant", "Constant expression"),
    Language("el", "EL", "Unified expression language from JSP / JSTL / JSF"),
    Language("header", "Header", "Header value"),
    Language("javaScript", "JavaScript", "JavaScript expression"),
    Language("jxpath", "JXPath", "JXPath expression"),
    Language("method", "Method", "Method call expression"),
    Language("mvel", "MVEL", "MVEL expression"),
    Language("ognl", "OGNL", "OGNL expression"),
    Language("groovy", "Groovy", "Groovy expression"),
    Language("property", "Property", "Property value"),
    Language("python", "Python", "Python expression"),
    Language("php", "PHP", "PHP expression"),
    Language("ref", "Ref", "Reference to a bean expression"),
    Language("ruby", "Ruby", "Ruby expression"),
    Language("simple", "Simple", "Simple expression language from Camel"),
    Language("spel", "Spring EL", "Spring expression language"),
    Language("sql", "SQL", "SQL expression"),
    Language("tokenize", "Tokenizer", "Tokenizing expression"),
    Language("xpath", "XPath", "XPath expression"),
    Language("xquery", "XQuery", "XQuery expression")
  )

  def languageArray: Array[String] = languages.map(_.id).toArray

  def nameAndLanguageArray: Array[Array[String]] = languages.map(l => Array(l.name, l.id)).toArray
}

case class Language(id: String, name: String, description: String)
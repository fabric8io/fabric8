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

import java.{util => ju}
import java.net.URL
import javax.xml.bind.{Marshaller, JAXBContext}
import javax.xml.parsers.{DocumentBuilder, DocumentBuilderFactory}

import collection.Map
import collection.immutable.TreeSet
import collection.mutable.HashMap
import collection.mutable.ListBuffer
import collection.JavaConversions._

import org.apache.camel.CamelContext
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.model.{RoutesDefinition, RouteDefinition, Constants}
import org.apache.camel.blueprint.{CamelEndpointFactoryBean => BlueprintCamelEndpointFactoryBean}
import org.apache.camel.spring.{CamelRouteContextFactoryBean, CamelEndpointFactoryBean, CamelContextFactoryBean}

import org.fusesource.scalate.util.{ClassLoaders, Logging, IOUtil}
import de.pdark.decentxml._

import org.xml.sax.{SAXParseException, ErrorHandler, InputSource}
import javax.xml.XMLConstants
import javax.xml.transform.{Source, TransformerFactory}
import javax.xml.transform.stream.{StreamSource, StreamResult}
import javax.xml.validation.{Schema, SchemaFactory}
import java.io._
import parser.PatchedXMLParser

case class XsdDetails(path: String, uri: String, aClass: Class[_]) {
  def classLoader = aClass.getClassLoader

}

trait SchemaFinder {
  def findSchema(xsd: XsdDetails): URL
}

object CamelNamespaces extends Logging {
  val springNS = "http://camel.apache.org/schema/spring"
  val blueprintNS = "http://camel.apache.org/schema/blueprint"

  val camelNamespaces = Set(springNS, blueprintNS)

  val springNamespace = new Namespace("", "http://www.springframework.org/schema/beans")
  val droolsNamespace = new Namespace("drools", "http://drools.org/schema/drools-spring")

  private var _schema: Schema = _

  lazy val elementsWithDescription: Set[String] = {
    val file = "camelDescriptionElements.txt"
    var text = findResource(file) match {
      case Some(url) =>
        IOUtil.loadText(url.openStream())
      case _ =>
        warn("Could not find file " + file + " on the class path")
        ""
    }
    text += " camelContext"
    val arr = text.split(' ')
    Set(arr:_*)
  }

  def nodesByNamespace(doc: Document, namespaceUri: String, localName: String): ju.List[Node] = {
    val filter = new NodeFilter[Node]() {
          override def matches(n: Node) = n match {
            case e: Element =>
              val ns = e.getNamespace
              // TODO this doesn't work with empty prefixes!
              if (!e.getName().equals(localName)) false else {
                val uri = getNamespaceURI(e)
                ns != null // && namespaceUri.equals(uri)
              }
            case _ => false
          }
        }
    return findNodes(doc, filter)
  }

  def findNodes(node: NodeWithChildren,  filter: NodeFilter[Node]): ju.List[Node] = {
    val answer = node.getNodes(filter)
    val children = node.getNodes()
    for (child <- children) {
      child match {
        case childElem: Element =>
          val childMatched = findNodes(childElem, filter)
          answer.addAll(childMatched)
        case _ =>
      }
    }
    return answer
  }

  def nodeWithNamespacesToText(parseNode: Node, namespacesNode: Element): String = {
    // we need to add any namespace prefixes defined in the root directory
    val copy = parseNode.copy()
    copy match {
      case e: Element =>

        parseNode match {
          case pe: Element =>
            moveCommentsIntoDescriptionElements(e, pe)
            addParentNamespaces(e, namespacesNode.getParent())
          case _ =>
        }
      case _ =>
    }
    val xmlText = xmlToText(copy)
    xmlText
  }

  protected def moveCommentsIntoDescriptionElements(e: Element, root: Element): Unit = {
    // lets iterate through finding all comments which are then added to a description node
    var idx = 0
    for (node <- e.getNodes().toArray) {
      node match {
        case c: Comment =>
          val token = c.getToken()
          if (token != null) {
            var text = token.getText.stripPrefix("<!--").stripSuffix("-->").trim()
            var descr = findOrCreateDescriptionOnNextElement(e, idx, root)
            if (descr == null) {
              // lets move the comment node to before the root element...
              warn("No description node found")
              e.removeNode(c)
              val grandParent = root.getParent
              if (grandParent != null) {
                grandParent.addNode(grandParent.nodeIndexOf(root), c)
              } else {
                warn("Cannot save the comment '" + text + "' as there's no parent in the DOM" )
              }
            } else {
              if (descr.getNodes().size() > 0) {
                text = "\n" + text
              }
              descr.addNode(new Text(text))
            }
          }
        case child: Element =>
          moveCommentsIntoDescriptionElements(child, root)
        case _ =>
      }
      idx += 1
    }
  }

  protected def findOrCreateDescriptionOnNextElement(element: Element, commentIndex: Int, root: Parent): Element = {
    // lets find the next peer element node and if it can contain a description lets use that
    val array = element.getNodes.toArray()
    for (i <- (commentIndex + 1).to(array.length - 1)) {
      array(i) match {
        case e: Element =>
          if (elementsWithDescription.contains(e.getName)) return findOrCreateDescrptionElement(e, root)
        case _ =>
      }
    }
    return findOrCreateDescrptionElement(element, root)
  }

  protected def findOrCreateDescrptionElement(element: Element, root: Parent): Element = {
    for (node <- element.getNodes()) {
      node match {
        case child: Element =>
          if (child.getName == "description")
            return child
        case _ =>
      }
    }
    val parent = element.getParentElement
    if (element == root || parent == null || parent == root) {
      null
    } else if (elementsWithDescription.contains(element.getName)) {
      // lets check for a namespace prefix
      val ebn = element.getBeginName
      val idx = ebn.indexOf(":")
      val name = if (idx > 0) ebn.substring(0, idx + 1) + "description" else "description"
      val description = new Element(name, element.getNamespace)
      element.addNode(0, description)
      description
    } else {
      findOrCreateDescrptionElement(parent, root)
    }
  }



  def xmlToText(node: Node): String = {
    val buffer = new StringWriter
    val writer = new XMLWriter(buffer)
    node.toXML(writer)
    writer.close()

/*
    val transformer = transformerFactory.newTransformer
    transformer.transform(new DOMSource(doc), new StreamResult(buffer))
*/
    buffer.toString
  }

  def getNamespaceURI(node: Node): String = {
    return node match {
      case e: Element =>
        val ns = e.getNamespace()
        if (ns != null) {
          val uri = ns.getURI
          if (uri == null || uri.length() == 0) {
            val uriAttr = if (ns.getPrefix == "") e.getAttributeValue("xmlns") else null
            if (uriAttr != null) {
              uriAttr
            }
            else getNamespaceURI(e.getParent)
          } else {
            uri
          }
        } else null
      case _ => return null
    }
  }

  def addParentNamespaces(element: Element,  parent: Parent): Unit = {
    parent match {
      case parentE: Element =>
        for (attr <- parentE.getAttributes) {
          val name = attr.getName
          if (name.startsWith("xmlns") && element.getAttributeValue(name) == null) {
            element.setAttribute(name, attr.getValue)
          }
        }
        addParentNamespaces(element, parentE.getParent)
      case _ =>
    }
  }


  def getOwnerDocument(node: Node): Document = {
    node match {
      case e: Element => e.getDocument
      case d: Document => d
      case _ => null
    }
  }

  def replaceChild(parent: Parent, newChild: Node, oldNode: Node): Unit = {
    val idx = parent.nodeIndexOf(oldNode)
    if (idx < 0) {
      parent.addNode(newChild)
    } else {
      parent.removeNode(idx)
      parent.addNode(idx, newChild)
    }
  }



  // TODO zap when Scalate 1.4 released....

  def findResource(name: String, classLoaders: Traversable[ClassLoader] = ClassLoaders.defaultClassLoaders): Option[URL] = {
    def tryLoadClass(classLoader: ClassLoader) = {
      try {
        classLoader.getResource(name)
      }
      catch {
        case e => null
      }
    }
    classLoaders.map(tryLoadClass).find(_ != null)
  }

    def loadSchemasWith(finder: SchemaFinder): Unit = {
      def fn(xsd: XsdDetails): URL = {
        finder.findSchema(xsd)
      }
      loadSchemas(fn)
    }

    def loadSchemas(fn: XsdDetails => URL): Unit = {
      val factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)

      val xsds = List(XsdDetails("camel-spring.xsd", "http://camel.apache.org/schema/spring/camel-spring.xsd", classOf[CamelEndpointFactoryBean]),
        XsdDetails("camel-blueprint.xsd", "http://camel.apache.org/schema/blueprint/camel-blueprint.xsd", classOf[BlueprintCamelEndpointFactoryBean]))

      val sources: Array[Source] = xsds.map {
        xsd =>
          import xsd._
          val url = fn(xsd)
          if (url != null) {
            new StreamSource(url.openStream(), uri)
          } else {
            println("Warning could not find local resource " + path + " on classpath")
            new StreamSource(uri)
          }
      }.toArray
      _schema = factory.newSchema(sources)
    }

    def camelSchemas: Schema = {
      if (_schema == null) {
        loadSchemas(xsd => xsd.classLoader.getResource(xsd.path))
      }
      _schema
    }
  }

  import CamelNamespaces._

case class XmlModel(contextElement: CamelContextFactoryBean,
                    doc: Document,
                    beans: Map[String, String],
                    node: Option[Node],
                    ns: String = CamelNamespaces.springNS,
                    justRoutes: Boolean = false,
                    routesContext : Boolean = false) extends Logging {

  /**
   * Returns the root element to be marshalled as XML
   */
  def marshalRootElement: AnyRef = {
    if (justRoutes) {
      val routes = new RoutesDefinition()
      routes.setRoutes(contextElement.getRoutes)
      routes
    } else {
       contextElement
    }
  }
  val hasMissingId = routeDefinitions.exists(_.getId == null)

  def getRouteDefinitionList: ju.List[RouteDefinition] = contextElement.getRoutes

  def routeDefinitions: List[RouteDefinition] = getRouteDefinitionList.toList

  /**
   *  Creates a new model using the given context
   */
  def update(newContext: CamelContextFactoryBean) = copy(contextElement = newContext)

  def camelContext: CamelContext = createContext(contextElement.getRoutes)

  /**
   * Returns the endpoint URIs used in the context
   */
  def endpointUris: Set[String] = {
    try {
      // we must use reflection for now until Camel supports the getEndpoints() method
      // https://issues.apache.org/jira/browse/CAMEL-3644
      val field = contextElement.getClass.getDeclaredField("endpoints")
      field.setAccessible(true)
      val endpoints = field.get(contextElement).asInstanceOf[ju.List[CamelEndpointFactoryBean]]
      val uris = ListBuffer[String]()
      if (endpoints != null) {
        uris ++= endpoints.map(_.getUri)
      }

      // lets detect any drools endpoints...
      val sessions = nodesByNamespace(doc, droolsNamespace.getURI, "ksession")
      if (sessions != null) {
        for (session <- sessions) {
          session match {
            case e: Element =>
              val node = e.getAttributeValue("node")
              val sid = e.getAttributeValue("id")
              if (node != null && node.length > 0 && sid != null && sid.length > 0) {
                val du = "drools:" + node + "/" + sid
                if (!uris.exists{_.startsWith(du)}) {
                  uris += du
                }
              }
            case _ =>
          }
        }
      }
      TreeSet(uris: _*)
    }
    catch {
      case e =>
        println("Caught: " + e)
        e.printStackTrace()
        Set()
    }
  }

  def endpointUriSet: ju.Set[String] = endpointUris

  /**
   * Returns a Java API for accessing the bean map
   */
  def beanMap: ju.Map[String, String] = beans

  def validate: ValidationHandler = {
    val v = new ValidationHandler()
    v.validate(doc)
    v
  }

  protected def createContext(routes: ju.Collection[RouteDefinition]): CamelContext = {
    val context = new DefaultCamelContext
    context.addRouteDefinitions(routes)
    context
  }
}

/**
 * Helper class for loading and saving XML for use at design time
 */
class RouteXml extends Logging {
  private var _jaxbContext: JAXBContext = _

  var classLoader: ClassLoader = classOf[CamelContextFactoryBean].getClassLoader

  var validating = false

  protected lazy val transformerFactory = TransformerFactory.newInstance
  protected lazy val documentBuilder = createDocumentBuilder

  protected def documentBuilder(handler: ErrorHandler) = {
    val db = createDocumentBuilder
    db.setErrorHandler(handler)
    db
  }

  def jaxbContext: JAXBContext = {
    if (_jaxbContext == null) {
      val packageName = Constants.JAXB_CONTEXT_PACKAGES + ":org.apache.camel.spring"
      _jaxbContext = JAXBContext.newInstance(packageName, classLoader)
    }
    _jaxbContext
  }

  def jaxbContext_=(value: JAXBContext): Unit = {
    _jaxbContext = value
  }


  protected def createExemplarDoc() = {
    val exemplar = "org/fusesource/camel/tooling/exemplar.xml"
    findResource(exemplar) match {
      case Some(url) =>
       parse(new XMLIOSource(url))
      case _ =>
        warn("Could not find file " + exemplar + " on the class path")
        val d = new de.pdark.decentxml.Document()
        d.addNode(new de.pdark.decentxml.Element("beans", springNamespace))
        d
    }
  }

  protected def parse(source: XMLSource): de.pdark.decentxml.Document = {
/*
    val parser = new DOMParser()
    parser.setFeature("http://apache.org/xml/features/dom/include-ignorable-whitespace", false)
    parser.parse(inputSource)
    return parser.getDocument
*/
    val parser = new PatchedXMLParser()
    return parser.parse(source)
  }

  def unmarshal(file: File): XmlModel = {
    val doc = if (file.exists) {
      parse(new XMLIOSource(file))
/*
      // lets find the header stuff
      val root = doc.getRootElement
      if (root != null) {
        val name = root.getNodeName()
        println("====== node name is: " + name)
        val text = IOUtil.loadTextFile(file)
        val idx = text.indexOf("<" + name)
        if (idx > 0) {
          header = text.substring(0, idx)
          println("header: " + header)
        }
      }
*/
    } else {
      createExemplarDoc()
    }
    val answer = unmarshal(doc, "XML File " + file)
    answer
  }

  def unmarshal(text: String): XmlModel = {
    val doc = if (text.trim().length > 0) {
      //documentBuilder.parse(new InputSource(new StringReader(text)))
      parse(new XMLStringSource(text))
    } else {
      createExemplarDoc()
    }
    unmarshal(doc, "Text")
  }

  def unmarshal(doc: Document): XmlModel = unmarshal(doc, "XML document " + doc)

  def unmarshal(doc: Document, message: => String): XmlModel = {
    val unmarshaller = jaxbContext.createUnmarshaller

    //("bean", springNamespace)
    var beans = new HashMap[String, String]()

    // lets pull out the spring beans...
    val beanElems = nodesByNamespace(doc, springNS, "bean")

    for (n <- beanElems) {
      n match {
        case e: Element =>
          val id = e.getAttributeValue("id")
          val cn = e.getAttributeValue("class")
          if (id != null && cn != null) {
            beans += (id -> cn)
          }
        case _ =>
      }
    }

    // now lets pull out the jaxb routes...
    val search = List((springNS, "routeContext"), (springNS, "camelContext"), (springNS, "routes"), (blueprintNS, "routeContext"), (blueprintNS, "camelContext"), (blueprintNS, "routes"))

    val found = search.flatMap {
      case (ns, en) =>
        val nodes = nodesByNamespace(doc, ns, en)
        nodes.size() match {
          case 0 =>
            None
            /*
            val root = doc.getRootElement
            // for root elements being the same...
            if (en == root.getLocalName && ns == root.getNamespaceURI)
              Some(root)
            else
            */

          case n =>
            if (n > 1) {
              warn(message + " contains " + n + " <camelContext> elements. Only the first wone will be used")
            }
            val node = nodes(0)
            if (node != null) Some(node) else None
        }
    }
    found match {
      case node :: _ =>
        val ns = getNamespaceURI(node)
        val parseNode = if (ns != springNS) {
          cloneAndReplaceNamespace(node, ns, springNS)
        } else {
          node
        }
        var justRoutes = false
        var routesContext = false
        val xmlText = nodeWithNamespacesToText(parseNode, node.asInstanceOf[Element])
        val context = unmarshaller.unmarshal(new StringReader(xmlText)) match {
          case sc: CamelContextFactoryBean =>
            debug("Found a valid CamelContextFactoryBean! " + sc)
            sc
          case rd: RoutesDefinition =>
            justRoutes = true
            val sc = new CamelContextFactoryBean()
            sc.setRoutes(rd.getRoutes)
            sc
          case rc: CamelRouteContextFactoryBean =>
            justRoutes = false
            routesContext = true
            val sc = new CamelContextFactoryBean()
            sc.setRoutes(rc.getRoutes)
            sc
          case bprc : org.apache.camel.blueprint.CamelRouteContextFactoryBean =>
            justRoutes = false
            routesContext = true
            val sc = new CamelContextFactoryBean()
            sc.setRoutes(bprc.getRoutes)
            sc
          case n =>
            warn("Unmarshalled not a CamelContext: " + n)
            new CamelContextFactoryBean
        }
        XmlModel(context, doc, beans, Some(node), ns, justRoutes, routesContext)

      case n =>
        info(message + " does not contain a CamelContext. Maybe the XML namespace is not spring: '" + springNS + "' or blueprint: '" + blueprintNS + "'?")
        // lets create a new collection
        XmlModel(new CamelContextFactoryBean(), doc, beans, None)
    }
  }

  protected def cloneAndReplaceNamespace(node: Node, oldNS: String, newNS: String): Node = {
    val answer = node.copy()
    replaceNamespace(answer, oldNS, newNS)
  }

  protected def replaceNamespace(node: Node, oldNS: String, newNS: String): Node = {
    var newNode = node
    node match {
      case e: Element =>
        val ns = getNamespaceURI(e)
        if (ns == oldNS) {
          val ns = e.getNamespace()
          if (ns != null) {
            if (ns.getURI == oldNS) {
              e.setNamespace(new Namespace(ns.getPrefix(), newNS))
            }
          }

          for (attr <- e.getAttributes) {
            if (attr.getName.startsWith("xmlns")) {
              val value = attr.getValue
              if (value != null && value == oldNS) {
                attr.setValue(newNS)
              }
            }
          }
        }
      case _ =>
    }
    newNode match {
      case nodeWithChildren: NodeWithChildren =>
        var list = nodeWithChildren.getNodes
        for (n <- list) {
            replaceNamespace(n, oldNS, newNS)
        }
      case _ =>
    }
    newNode
  }

  def marshal(file: File, context: CamelContextFactoryBean): Unit = marshal(file) {
      m =>
      m.update(context)
  }

  def marshal(file: File, context: CamelContext): Unit = marshal(file) {
      m =>
      copyRoutesToElement(context, m.contextElement)
      m
  }

  def copyRoutesToElement(routeDefinitionList: ju.List[RouteDefinition], contextElement: CamelContextFactoryBean): Unit = {
    val routes = contextElement.getRoutes
    routes.clear
    routes.addAll(routeDefinitionList)
  }

  def copyRoutesToElement(context: CamelContext, contextElement: CamelContextFactoryBean): Unit = {
    context match {
      case mcc: ModelCamelContext => copyRoutesToElement(mcc.getRouteDefinitions, contextElement);
      case _=> println("Invalid camel context!") }
  }

  /**
   * Loads the given file then updates the route definitions from the given list then stores the file again
   */
  def marshal(file: File, routeDefinitionList: ju.List[RouteDefinition]): Unit = {
    marshal(file) {
        model: XmlModel =>
        copyRoutesToElement(routeDefinitionList, model.contextElement)
        model
    }
  }

  def marshal(file: File)(fn: XmlModel => XmlModel): Unit = {
    // lets load the file first in case its been edited since we last loaded it
    val model = unmarshal(file)
    marshal(file, fn(model))
  }

  def marshalToText(text: String, routeDefinitionList: ju.List[RouteDefinition]): String = {
    marshalToText(text) {
        model: XmlModel =>
        copyRoutesToElement(routeDefinitionList, model.contextElement)
        model
    }
  }

  def marshalToText(text: String)(fn: XmlModel => XmlModel): String = {
    val model = unmarshal(text)
    marshalToText(fn(model))
  }

  def marshal(file: File, model: XmlModel): Unit = {
    marshalToDoc(model)
    writeXml(model.doc, file)
  }

  def marshalToText(model: XmlModel): String = {
    marshalToDoc(model)
    xmlToText(model.doc)
  }

  protected def replaceCamelElement(docElem: Element, camelElem: Node, oldNode: Node) {
    replaceChild(docElem, camelElem, oldNode)

    // lets replace the camel namespace, copying any namespace from the old node as well
    camelElem match {
      case camelElement: Element =>
        oldNode match {
          case oldElement: Element =>
            for (attr <- oldElement.getAttributes) {
                if (attr.getName.startsWith("xmlns:")) {
                  camelElement.setAttribute(attr.getName, attr.getValue)
                }
            }
          case _ =>
        }
      case _ =>
    }
  }

  /**
   * Marshals the model to XML and updates the model's doc property to contain the
   * new marshalled model
   */
  def marshalToDoc(model: XmlModel): Unit = {
    val marshaller = jaxbContext.createMarshaller
    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, java.lang.Boolean.TRUE)
    try {
      marshaller.setProperty("com.sun.xml.bind.indentString", "  ")
    }
    catch {
      case e => debug("Property not supported: " + e)
    }

    val value = model.marshalRootElement
    val doc = model.doc
    val docElem = doc.getRootElement

    // JAXB only seems to do nice whitespace/namespace stuff when writing to stream
    // rather than DOM directly
    // marshaller.marshal(value, docElem)

    val buffer = new StringWriter
    marshaller.marshal(value, buffer)

    // now lets parse the XML and insert the root element into the doc
    var xml = buffer.toString
    if (model.ns != springNS) {
      xml = xml.replaceAll(springNS, model.ns)
    }
    val camelDoc =  parse(new XMLStringSource(xml))
    var element: Node = camelDoc.getRootElement
    // TODO
    //val camelElem = doc.importNode(element, true)
    val camelElem = element

    if (model.routesContext && camelDoc.getRootElement.getName == "camelContext") {
      camelDoc.getRootElement.setName("routeContext")
    }
    if (model.justRoutes) {
      replaceChild(doc, camelElem, docElem)
    } else {
      model.node match {
        case Some(n) =>
          replaceCamelElement(docElem, camelElem, n)
        case _ =>
          docElem.addNode(camelElem)
      }
    }
  }

  def writeXml(doc: Document, file: File): Unit = {
    val parentDir = file.getParentFile
    if (parentDir != null) parentDir.mkdirs


    val writer = new XMLWriter(new FileWriter(file))
    doc.toXML(writer)
    writer.close()
/*
    val format = new OutputFormat(doc)
    format.setPreserveSpace(true)
    val out = new FileOutputStream(file)
    try {
      val serial = new XMLSerializer(out, format);
      serial.asDOMSerializer()
      serial.serialize(doc)
      out.flush()
    }
    finally {
      out.close()
    }
*/
/*
    val transformer = transformerFactory.newTransformer
    transformer.transform(new DOMSource(doc), new StreamResult(file))
*/
  }


  protected def createDocumentBuilder: DocumentBuilder = {
    val JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage"
    val JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource"
    val W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema"

    val dbf = DocumentBuilderFactory.newInstance
    if (validating) {
      try {
        dbf.setAttribute(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
        dbf.setAttribute(JAXP_SCHEMA_SOURCE, camelSchemas)
        dbf.setValidating(validating)
      }
      catch {
        case e => // ignore
      }
    }
    dbf.setExpandEntityReferences(false)
    dbf.setIgnoringComments(false)
    dbf.setIgnoringElementContentWhitespace(false)
    dbf.setCoalescing(false)
    dbf.setNamespaceAware(true)
    dbf.newDocumentBuilder
  }



}

case class ValidationException(errors: List[SAXParseException], fatalErrors: List[SAXParseException], warnings: List[SAXParseException])
        extends Exception("Validation failed: " + ((errors ++ fatalErrors).map(_.getMessage).mkString(", "))) {

  def userMessage = {
    var text = (errors ++ fatalErrors).map(_.getMessage).mkString(", ")
    val idx = text.indexOf(":")
    if (text.startsWith("cvc-complex-type") && idx > 0) {
      text = text.drop(idx + 1).trim
    }
    for (uri <- camelNamespaces) {
      text = text.replaceAllLiterally("\"" + uri + "\":", "")
    }
    text
  }
}

class ValidationHandler extends ErrorHandler {
  var warnings = List[SAXParseException]()
  var errors = List[SAXParseException]()
  var fatalErrors = List[SAXParseException]()

  def warning(e: SAXParseException): Unit = {
    warnings :+= e
  }

  def fatalError(e: SAXParseException): Unit =  {
    fatalErrors :+= e
  }

  def error(e: SAXParseException): Unit =  {
    errors :+= e
  }

  def userMessage: String = {
    var text = (errors ++ fatalErrors).map(_.getMessage).mkString(", ")
    val idx = text.indexOf(":")
    if (text.startsWith("cvc-complex-type") && idx > 0) {
      text = text.drop(idx + 1).trim
    }
    for (uri <- camelNamespaces) {
      text = text.replaceAllLiterally("\"" + uri + "\":", "")
    }
    text
  }

  def validate(doc: Document): Unit = {
    val validator = camelSchemas.newValidator()
    validator.setErrorHandler(this)

    def validate(e: Element): Unit = {
      val uri = getNamespaceURI(e)
      if (uri != null && camelNamespaces.contains(uri)) {
        val text = nodeWithNamespacesToText(e, e)
        validator.validate(new StreamSource(new StringReader(text)))
      } else {
        val children = e.getNodes
        for (node <- e.getNodes()) {
          node match {
            case c: Element => validate(c)
            case _ =>
          }
        }
      }
    }
    validate(doc.getRootElement)
  }

  def hasErrors: Boolean = {
    !errors.isEmpty || !fatalErrors.isEmpty
  }

  def checkForErrors: Unit = {
     if (hasErrors) {
       throw new ValidationException(errors, fatalErrors, warnings)
     }
  }
}
package by.amushinsky.pizero

import java.io.InputStream
import java.util.Scanner
import java.util.regex.Pattern

import scala.xml.Elem
import scala.xml.Group
import scala.xml.MetaData
import scala.xml.Node
import scala.xml.NodeSeq
import scala.xml.NodeSeq.seqToNodeSeq

import org.json4s.JsonAST.JArray
import org.json4s.JsonAST.JField
import org.json4s.JsonAST.JObject
import org.json4s.JsonAST.JString
import org.json4s.JsonAST.JValue

package object xml {

  /**
   * Cut chunks of XML from provided [[java.io.InputStream]] between specified
   * tag including the tag itself.
   *
   * @param input [[java.io.InputStream]] of the valid XML document
   * @param tagName name of the XML tag
   * @return iterator containing all the chunks from XML
   */

  def produseXmlChunks(input: InputStream, tagName: String): Iterator[String] = {
    val scanner = new Scanner(input)
    val pattern = Pattern.compile(s"<$tagName.+?</$tagName>", Pattern.DOTALL)
    new Iterator[String] {
      var _next: String = null
      def hasNext = _next != null
      def next = { val result = _next; _next = scanner.findWithinHorizon(pattern, 0); result }
      next
    }
  }

  /**
   * Convert given XML to JSON.
   *
   * @param xml XML document
   * @return JSON
   */
  def xml2Json(xml: NodeSeq): JValue = {

    def isEmpty(node: Node) = node.child.isEmpty

    /* Checks if given node is leaf element. For instance these are considered leafs:
     * <foo>bar</foo>, <foo>{ doSomething() }</foo>, etc.
     */
    def isLeaf(node: Node) = {
      def descendant(n: Node): List[Node] = n match {
        case g: Group => g.nodes.toList.flatMap(x => x :: descendant(x))
        case _        => n.child.toList.flatMap { x => x :: descendant(x) }
      }

      !descendant(node).find(_.isInstanceOf[Elem]).isDefined
    }
    def isArray(nodeNames: Seq[String]) = nodeNames.size != 1 && nodeNames.toList.distinct.size == 1
    def directChildren(n: Node): NodeSeq = n.child.filter(c => c.isInstanceOf[Elem])
    def nameOf(n: Node) = (if (n.prefix ne null) n.prefix + ":" else "") + n.label
    def buildAttrs(n: Node) = n.attributes.map((a: MetaData) => (a.key, XValue(a.value.text))).toList

    sealed abstract class XElem extends Product with Serializable
    case class XValue(value: String) extends XElem
    case class XLeaf(value: (String, XElem), attrs: List[(String, XValue)]) extends XElem
    case class XNode(fields: List[(String, XElem)]) extends XElem
    case class XArray(elems: List[XElem]) extends XElem

    def toJValue(x: XElem): JValue = x match {
      case XValue(s) => JString(s)
      case XLeaf((name, value), attrs) => (value, attrs) match {
        case (_, Nil)         => toJValue(value)
        case (XValue(""), xs) => JObject(mkFields(xs))
        case (_, xs)          => JObject(JField(name + "Description", JObject((name, toJValue(value)) :: mkFields(xs))) :: Nil)
      }
      case XNode(xs)     => JObject(mkFields(xs))
      case XArray(elems) => JArray(elems.map(toJValue))
    }

    def mkFields(xs: List[(String, XElem)]) =
      xs.flatMap {
        case (name, value) => (value, toJValue(value)) match {
          // This special case is needed to flatten nested objects which resulted from
          // XML attributes. Flattening keeps transformation more predictable.
          case (XLeaf(v, x :: xs), o: JObject) => o.obj
          case (_, json)                       => JField(name, json) :: Nil
        }
      }

    def buildNodes(xml: NodeSeq): List[XElem] = xml match {
      case n: Node =>
        if (isEmpty(n)) XLeaf((nameOf(n), XValue("")), buildAttrs(n)) :: Nil
        else if (isLeaf(n)) XLeaf((nameOf(n), XValue(n.text)), buildAttrs(n)) :: Nil
        else {
          val children = directChildren(n)
          XNode(buildAttrs(n) ::: children.map(nameOf).toList.zip(buildNodes(children))) :: Nil
        }
      case nodes: NodeSeq =>
        val allLabels = nodes.map(_.label)
        if (isArray(allLabels)) {
          val arr = XArray(nodes.toList.flatMap { n =>
            if (isLeaf(n) && n.attributes.length == 0) XValue(n.text) :: Nil
            else buildNodes(n)
          })
          XLeaf((allLabels(0), arr), Nil) :: Nil
        } else nodes.toList.flatMap(buildNodes)
    }

    buildNodes(xml) match {
      case List(x @ XLeaf(_, _ :: _)) => toJValue(x)
      case List(x)                    => JObject(JField(nameOf(xml.head), toJValue(x)) :: Nil)
      case x                          => JArray(x.map(toJValue))
    }
  }

}
package by.amushinsky.pizero.scalacheck

import java.io.File
import java.io.FileInputStream
import java.util.regex.Pattern

import org.json4s.jackson.JsonMethods.pretty
import org.json4s.jackson.JsonMethods.render
import org.scalacheck.Gen
import org.scalacheck.Prop.BooleanOperators
import org.scalacheck.Prop.forAll
import org.scalacheck.Prop.propBoolean
import org.scalacheck.Properties

object XmlSpecification extends Properties("XML") {

  val input = Gen.const((new FileInputStream(new File("src/test/resources/input.xml")), "book"))

  property("ProduseXmlChunksPattern") = forAll(input) {
    case (file, tagName) =>
      val chunks = by.amushinsky.pizero.xml.produseXmlChunks(file, tagName)
      val pattern = Pattern.compile(s"^<$tagName.+?</$tagName>$$", Pattern.DOTALL)
      val docs = chunks.take(12)
      docs.forall { pattern.matcher(_).matches() } :| s"result not match pattern ${pattern}" && (!chunks.hasNext)
  }

  val xml = Gen.const(
    <Book id="1001" avaible="true">
      <Author>
        <AuthorName>Name</AuthorName>
        <AuthorAge>35</AuthorAge>
      </Author>
      <Year>1995</Year>
      <Publisher id="103">West</Publisher>
    </Book>)

  val json = """{
  "Book" : {
    "id" : "1001",
    "avaible" : "true",
    "Author" : {
      "AuthorName" : "Name",
      "AuthorAge" : "35"
    },
    "Year" : "1995",
    "PublisherDescription" : {
      "Publisher" : "West",
      "id" : "103"
    }
  }
}"""

  property("xml2json") = forAll(xml) { xml => {
      pretty(render(by.amushinsky.pizero.xml.xml2Json(xml))).equals(json)
    }
  }

}
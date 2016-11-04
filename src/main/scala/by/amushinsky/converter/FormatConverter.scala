package by.amushinsky.converter

import org.json4s.Xml.toJson
import org.json4s.JsonAST._
import scala.xml.pull.XMLEventReader
import scala.xml.pull.EvElemStart
import scala.xml.pull.EvElemEnd
import scala.xml.pull.EvText

object FormatConverter {

  def xml2json(xml: scala.xml.Elem): JValue = toJson(xml).transformField {
    case field @ JField(key, JString(str)) if key.contains("ZIP")        => field
    case field @ JField(key, JString(str)) if key.contains("Postal")     => field
    case field @ JField(key, JString(str)) if key.contains("Phone")      => field
    case JField(key, JString(str)) if str.matches("[-+]?\\d+(\\.\\d+)?") => JField(key, JDecimal(BigDecimal(str)))
  }

}
package by.amushinsky.pizero

import org.json4s.JsonAST.JValue
import org.json4s.JsonAST.JObject
import org.json4s.JsonAST.JField
import org.json4s.JsonAST.JArray

package object json {

  /** If JSON contains object with equally named fields convert them into array. */
  def groupSameFields(jValue: JValue): JValue = jValue match {
    case JObject(jFields) => {
      val gropedFields = jFields.groupBy { case JField(key, _) => key }
      val newJFields = gropedFields map {
        case (_, List(JField(key, value))) => key -> groupSameFields(value)
        case (key, fields)                 => key -> JArray(fields.map { case JField(_, value) => groupSameFields(value) })
      }
      JObject(newJFields.toList)
    }
    case any => any
  }
  
  
  
}
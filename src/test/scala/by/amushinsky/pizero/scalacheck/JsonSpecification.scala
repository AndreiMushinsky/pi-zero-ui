package by.amushinsky.pizero.scalacheck

import org.scalacheck.Properties
import org.scalacheck.Gen
import org.json4s.JsonAST.JObject
import org.json4s.JsonAST.JField
import org.json4s.JsonAST.JInt
import org.scalacheck.Prop.forAll
import org.json4s.JsonAST.JArray

object JsonSpecification extends Properties("JSON") {

  
  val ungrouped = Gen.const(
    JObject(
      JField("list",
        JObject(
          JField("elem", JInt(1)) ::
            JField("elem", JInt(2)) :: Nil))))

  property("GroupSameFields") = forAll(ungrouped) { json =>
    by.amushinsky.pizero.json.groupSameFields(json) match {
      case JObject(
        JField("list", JObject(
          JField("elem", JArray(a::b::Nil)) :: Nil)) :: Nil) => true
      case _ => false
    }
  }
}
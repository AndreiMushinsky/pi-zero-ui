package by.amushinsky.elastic

import com.sksamuel.elastic4s.ElasticClient
import scala.annotation.tailrec
import org.json4s.JsonAST.JValue
import org.json4s.JsonAST.JArray
import scala.xml.XML
import by.amushinsky.converter.FormatConverter
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import scala.io.Source
import java.io.FileWriter
import java.util.Scanner
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.regex.Pattern
import by.amushinsky.converter.SqlConverter.LeftJoin
import com.sksamuel.elastic4s.ElasticDsl._
import org.json4s.JsonAST.JInt
import scala.concurrent.Future

trait ElasticServiceElastic4sComponent extends ElasticServiceComponent {
  val esClient: ElasticClient
  def elasticLocator = new ElasticLocatorElastic4s(esClient)
  def elasticUpdater = new ElasticUpdaterElastic4s(esClient)

  class ElasticLocatorElastic4s(esClient: ElasticClient) extends ElasticLocator {
    def schema = esClient execute { getMapping("*") }
  }

  class ElasticUpdaterElastic4s(esClient: ElasticClient) extends ElasticUpdater {
    import org.json4s.Xml.toJson

    val bulkSize = 20000

    def uploadFromFile(input: InputStream, ind: String, typ: String) {
      val pattern = Pattern.compile(s"<$typ.+?</$typ>", Pattern.DOTALL)
      val scanner = new Scanner(input)
      var docs: List[String] = Nil
      var doc: String = null
      do {
        doc = scanner.findWithinHorizon(pattern, 100000)
        if (doc == null) clear(docs)
        else {
          val xml = XML.loadString(doc)
          val json = compact(render(FormatConverter.xml2json(xml)))
          if (docs.size > bulkSize) {
            clear(json :: docs); docs = Nil
          } else {
            docs = (json :: docs)
          }
        }
      } while (doc != null)

      def clear(docs: List[String]) {
        println(s"Submitting ${docs.size}")
        esClient execute { bulk { docs.map { index into ind / typ source _ } } }
      }
    }

    import scala.concurrent.ExecutionContext.Implicits.global
    def fromSql(leftJoin: LeftJoin): Future[String] = leftJoin match {
      case LeftJoin(Array(ind1, typ1), Array(ind2, typ2), fld1, fld2, lim) => {
        println("About to query")
        val pivot = esClient execute {
          search in ind1 -> typ1 query { matchAllQuery } limit lim
        }
        val pivotSource = pivot.map { _.hits.map { hit => hit.getSourceAsString } }
        val results = pivotSource.map { sourceValues =>
          val requests = sourceValues.map { src =>
            val json = parse(src)
            println(pretty(render(json)))
            val jValue = fld1.split("\\.").foldLeft(json) { (jVal, field) => jVal \ field }
            println(jValue)
            val request = jValue match {
              case JArray(values) => {
                val keys = values.map {
                  case JInt(key) => key
                  case _         => throw new IllegalStateException("LEFT JOIN supports only integer keys")
                }
                search in ind2 -> typ2 query { termsQuery(fld2, keys) } limit lim
              }
              case JInt(key) => search in ind2 -> typ2 query { termQuery(fld2, key) } limit lim
              case _         => throw new IllegalStateException("LEFT JOIN supports only integer keys")
            }
            request
          }
          esClient execute { multi { requests } }
        }
        for {
          pivot <- pivotSource
          result <- results
          res <- result
        } yield res.items
          .map { _.item.getResponse.hits.map { _.getSourceAsString }.mkString("[", ",", "]") }
          .zip(pivot)
          .map{ case (joined, original) => s"{$original,joined: $joined}" }
          .mkString("[", ",", "]")
      }
    }

  }
}
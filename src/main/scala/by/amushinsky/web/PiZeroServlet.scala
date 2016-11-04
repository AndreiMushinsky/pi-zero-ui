package by.amushinsky.web

import org.scalatra._
import org.scalatra.servlet._
import org.json4s.{ DefaultFormats, Formats }
import org.scalatra.json._
import java.io.File
import org.scalatra.scalate.ScalateSupport
import by.amushinsky.elastic.ElasticServiceComponent
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.io.Source
import java.util.zip.GZIPInputStream
import java.io.InputStream
import java.io.FileInputStream
import java.net.URLDecoder
import java.util.regex.Pattern
import by.amushinsky.converter.SqlConverter

trait PiZeroServlet extends ScalatraServlet with FileUploadSupport with ScalateSupport with JacksonJsonSupport with FutureSupport {

  this: ElasticServiceComponent =>

  configureMultipartHandling(MultipartConfig(maxFileSize = Some(2 * 1024 * 1024 * 1024)))

  protected implicit lazy val jsonFormats: Formats = DefaultFormats
  protected implicit def executor: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  get("/") {
    contentType = "text/html"
    jade("index")
  }
  
  get("/sql") {
    contentType = "application/json"
    val sql = URLDecoder.decode(params("sql"), "UTF-8")
    Source.fromURL(s"http://localhost:9200/_sql?sql=${params("sql")}").mkString
  }

  get("/schema") {
    contentType = "application/json"
    elasticLocator.schema.map {
      _.mappings.flatMap {
        case (index, rest) => {
          val data = rest.map { case (_, info) => info.source().string() }
          data.map { parse(_) }.map {
            case JObject(JField(typ, props: JObject) :: Nil) =>
              ("index" -> index) ~ ("type" -> typ) ~ props
          }
        }
      }
    }
  }

  post("/upload") {
    val uploaded = fileParams("thefile")
    val filePath = s"/tmp/${uploaded.name}"
    uploaded.write(new File(filePath))
    
    val source = params.get("gzip") match {
      case Some("on") => new GZIPInputStream(new FileInputStream(filePath))
      case _ => new FileInputStream(filePath)
    }
    
    elasticUpdater.uploadFromFile(source, params("index"), params("type"))
    redirect("/")
  }
}

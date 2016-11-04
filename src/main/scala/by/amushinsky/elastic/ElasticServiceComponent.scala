package by.amushinsky.elastic

import scala.collection.immutable.HashMap
import com.sksamuel.elastic4s.mappings.GetMappingsResult
import scala.concurrent.Future
import scala.io.Source
import java.io.InputStream
import by.amushinsky.converter.SqlConverter.LeftJoin

trait ElasticServiceComponent {
  def elasticLocator: ElasticLocator
  def elasticUpdater: ElasticUpdater
  
  trait ElasticLocator {
    def schema: Future[GetMappingsResult]
  }
  
  trait ElasticUpdater {
    def fromSql(leftJoin: LeftJoin): Future[String]
    def uploadFromFile(input: InputStream, index: String, typ: String)
  }
}
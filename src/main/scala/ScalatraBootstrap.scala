import by.amushinsky.web._
import org.scalatra._
import javax.servlet.ServletContext
import by.amushinsky.elastic.ElasticServiceElastic4sComponent
import com.sksamuel.elastic4s.ElasticClient

class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext) {
    val servlet = new PiZeroServlet with ElasticServiceElastic4sComponent {
      val esClient = ElasticClient.remote("localhost", 9300)
    }
    context mount(servlet, "/*")
  }
}

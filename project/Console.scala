object Console
{

  protected val imports =
    """
      | import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem}
      | import akka.stream.ActorMaterializer
      | import akka.stream.scaladsl._
      | import akka.http.scaladsl.model._
      | import akka.http.scaladsl.model.Uri.Query
      | import akka.http.scaladsl.Http
      | HttpMethods._
      |
      | import scala.util._
      | import scala.concurrent.duration._
      |
      | import org.denigma.kappa._
      | import org.denigma.kappa.WebSim
      | import org.denigma.kappa.WebSim._
      | import org.denigma.kappa.notebook.services.WebSimClient
      | import de.heikoseeberger.akkahttpcirce.CirceSupport
      | import io.circe._
      | import io.circe.generic.auto._
      | import io.circe.parser
      | import io.circe.syntax._
    """.stripMargin

  protected val akka =
    """
      | implicit val system = ActorSystem("my-system")
      | implicit val materializer = ActorMaterializer()
      | implicit val executionContext = system.dispatcher
    """.stripMargin

  protected val loaders =
    """
      | val tester = new Tester()
    """.stripMargin

  val predef = imports + akka + loaders

  val out = s"""ammonite.repl.Main.run(\"\"\"${predef}\"\"\")"""
}
object Console
{

  protected val libs =
    """
      |load.ivy("com.lihaoyi" %% "fastparse" % "0.3.7")
      |load.ivy("com.typesafe.akka" %% "akka-stream" % "2.4.2")
      |load.ivy("com.typesafe.akka" %% "akka-http-core" % "2.4.2")
      |load.ivy("com.typesafe.akka" %% "akka-http-experimental" % "2.4.2")
      |load.ivy("io.circe" %% "circe-core" % "0.4.1")
      |load.ivy("io.circe" %% "circe-generic" % "0.4.1")
      |load.ivy("io.circe" %% "circe-parser" % "0.4.1")
      |load.ivy("de.heikoseeberger" %% "akka-http-circe" % "1.6.0")
      |//SBOL
      |load.ivy("org.sbolstandard" % "libSBOLj" % "2.0.0")
      |//matrix
      |load.ivy("org.scalanlp" %% "breeze" % "0.12")
      |//scala.rx
      |load.ivy("com.lihaoyi" %% "scalarx" % "0.3.1")
      |
      |@
    """.stripMargin

  protected val imports =
    """
      | import scala.concurrent.duration._
      | import scala.util._
      |
      | import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem}
      | import akka.stream.ActorMaterializer
      | import akka.stream.scaladsl._
      | import akka.http.scaladsl.model._
      | import akka.http.scaladsl.model.Uri.Query
      | import akka.http.scaladsl.Http
      |
      | import de.heikoseeberger.akkahttpcirce.CirceSupport
      | import io.circe._
      | import io.circe.generic.auto._
      | import io.circe.parser
      | import io.circe.syntax._
      | import org.denigma.kappa.notebook._
      |
      |import better.files._
      |import java.io.{File => JFile}
    """.stripMargin

  protected val akka =
    """
      | implicit val system = ActorSystem("my-system")
      | implicit val materializer = ActorMaterializer()
      | implicit val executionContext = system.dispatcher
    """.stripMargin

  protected val loaders =
    """
      | val host = "http://localhost:8080/v1/"
    """.stripMargin


  val predef = libs + imports + akka + loaders

  val out = s"""ammonite.repl.Main.run(\"\"\"${predef}\"\"\")"""
}
package org.denigma.kappa.messages

import boopickle.Default._
import org.denigma.controls.charts.{LineStyles, Point, Series}
import org.denigma.kappa.notebook.parsers.ChartParser
import scala.collection.immutable._

object KappaMessages {

  class Message

  object Container {
    def apply(message: Message): Container = Container(List(message))
  }
  case class Container(messages: List[Message]) extends Message
  {
    lazy val run: List[RunParameters] = messages.collect{ case message: RunParameters => message}
    lazy val load: List[Load] = messages.collect{ case message: Load => message}
    lazy val console: List[Console] = messages.collect{ case message: Console => message}
    lazy val code: List[Code] = messages.collect{ case message: Code => message}
    lazy val charts: List[Chart] = messages.collect{ case message:Chart => message}
  }

  case class Load(name: String = "abc") extends Message

  object Code
  {
    def apply(lines: List[String]): Code = Code(lines.foldLeft("")( (acc, el) => acc + (if(el.endsWith("\n")) el else el + "\n")))
    lazy val empty: Code = Code("")
  }
  case class Code(text: String) extends Message{
    lazy val lines = text.split("\n")
    def isEmpty: Boolean = text == ""
  }

  object Console{
    def apply(text: String): Console = Console(text.split("\n").toList)
    lazy val empty = Console(List.empty)
  }

  case class Console(lines: List[String]) extends Message
  {
    val text: String = lines.fold("")((acc, b)=> acc + b + "\n")
    def isEmpty: Boolean = lines.isEmpty
  }

  object Chart {
    lazy val empty = Chart(List.empty)
    def parse(output: Output): Chart = this.parse(output.lines)
    def parse(lines: Vector[String]): Chart = new ChartParser(lines).chart
  }

  case class Chart(series: List[KappaSeries]) extends Message
  {
    def isEmpty: Boolean = series.isEmpty
  }

  object Output {
    lazy val empty = Output(Vector.empty)
  }
  case class Output(lines: Vector[String]) extends Message {
    val text: String = lines.fold("")((acc, b)=> acc + b + "\n")
    def isEmpty: Boolean = lines.isEmpty
  }

  case class RunParameters(
                            fileName: String = "model.ka",
                            events: Option[Int] = Some(10000),
                            time: Option[Int] = None,
                            points: Int = 250,
                            output: Option[String] = None, //"" means same as file name
                            flow: Option[String] = None,
                            causality: Option[String] = None,
                            gluttony: Boolean = false,
                            implicitSignature: Boolean = false
                          ) extends Message {

    lazy val kaname: String = if (fileName.endsWith(".ka")) fileName else fileName+".ka"

    lazy val outputName: String =  output.getOrElse(kaname.replace(".ka", ".out"))

    protected def optSeq( couples: (Boolean, String) *): scala.Seq[String] = couples.collect{ case (true, value) => value}

    def optional: scala.Seq[String] = optSeq( (gluttony,"--gluttony"), (implicitSignature, "--implicit-signature"))


  }

  object KappaSeries {

    import scala.util.Random

    def randomColor() = s"rgb(${Random.nextInt(255)},${Random.nextInt(255)},${Random.nextInt(255)})"

    def randomLineStyle() = LineStyles(randomColor(), 4 ,"none" , 1.0)

  }

  case class KappaSeries(title: String, points: List[Point], style: LineStyles = KappaSeries.randomLineStyle()) extends Series

}

class KappaPicklers {

  import KappaMessages._

  implicit val datePickler = transformPickler[java.util.Date, Long](_.getTime, t => new java.util.Date(t))
  implicit val pointPickler = generatePickler[Point]
  implicit val lineStylesPickler = generatePickler[LineStyles]
  implicit val series = generatePickler[KappaSeries]

  object Single{
    implicit val messagePickler = compositePickler[Message]
      .addConcreteType[Code]
      .addConcreteType[Console]
      .addConcreteType[Chart]
      .addConcreteType[RunParameters]
      .addConcreteType[Output]
      .addConcreteType[Load]
  }
  import Single._

  implicit val kappaMessagePickler = compositePickler[Message]
    .addConcreteType[Code]
    .addConcreteType[Console]
    .addConcreteType[Chart]
    .addConcreteType[RunParameters]
    .addConcreteType[Load]
    .addConcreteType[Output]
    .addConcreteType[Container]


}

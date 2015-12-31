package org.denigma.kappa.messages

import boopickle.Default._
import org.denigma.controls.charts.{LineStyles, Point, Series}
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
    val text: String = lines.fold("")((a, b)=> a + "\n" + b)
    def isEmpty: Boolean = lines.isEmpty
  }

  object Chart {
    lazy val empty = Chart(List.empty)
  }

  case class Chart(series: List[KappaSeries]) extends Message
  {
    def isEmpty: Boolean = series.isEmpty
  }

  case class RunParameters(
                            fileName: String = "model.ka",
                            events: Option[Int] = Some(10000),
                            time: Option[Int] = None,
                            points: Int = 250,
                            chart: Option[String] = Some(""), //"" means same as file name
                            flow: Option[String] = None,
                            causality: Option[String] = None,
                            gluttony: Boolean = false,
                            deduceSignatures: Boolean = false
                          ) extends Message {

    lazy val kaname: String = if (fileName.endsWith(".ka")) fileName else fileName+".ka"
    lazy val chartName: String =  chart.getOrElse(kaname.replace(".ka", ".out"))

/*    def maxEvents: String = events.map(ev =>s"-e $ev").getOrElse("")
    def maxTime: String = time.map(t=> "-t $t").getOrElse("")*/

    def hasGluttony: String = if(gluttony) "--gluttony" else ""

    def implicitSignature = if(deduceSignatures) "--implicit-signature" else ""

    def flags = s"$hasGluttony $implicitSignature"

//    def parameters(KaSim: String) = s"$KaSim -i $kaname $maxEvents $maxTime $points -o $chartName $flags"

  }
  //lazy val defaultRunParameters = RunParameters(Some(10000), Time(, 1000))

  object KappaSeries {

    //val colors = Vector("green", "red", "pink", "blue", "lightblue", "violet", "cyan", "navy", "black", "rose")
    import org.denigma.binding.extensions._
    import scala.util.Random
    def randomColor() = s"rgb(${Random.nextInt(255)},${Random.nextInt(255)},${Random.nextInt(255)})"

    def randomLineStyle() = LineStyles(randomColor(), 4 ,"none" , 1.0)

  }

  case class KappaSeries(title: String, points: List[Point], style: LineStyles = KappaSeries.randomLineStyle()) extends Series


  // %%KaSim("-i", kaname, "-e",events,"-p", points, "-o", chart, "-d", outPutFolder)
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
      .addConcreteType[Load]
  }
  import Single._

  implicit val kappaMessagePickler = compositePickler[Message]
    .addConcreteType[Code]
    .addConcreteType[Console]
    .addConcreteType[Chart]
    .addConcreteType[RunParameters]
    .addConcreteType[Load]
    .addConcreteType[Container]


}

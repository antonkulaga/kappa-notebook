package org.denigma.kappa.messages

import boopickle.Default._
import org.denigma.controls.charts.{LineStyles, Point, Series}

object KappaMessages {

  class Message
  case class Load(name: String = "abc") extends Message
  case class Code(code: String) extends Message
  case class Console(lines: List[String]) extends Message
  case class Chart(series: List[KappaSeries]) extends Message
  case class KappaSeries(title: String, points: List[Point], style: LineStyles) extends Series

}

class KappaPicklers {

  import KappaMessages._

  implicit val datePickler = transformPickler[java.util.Date, Long](_.getTime, t => new java.util.Date(t))


  implicit val pointPickler = generatePickler[Point]
  implicit val lineStylesPickler = generatePickler[LineStyles]
  implicit val series = generatePickler[KappaSeries]

  implicit val kappaMessagePickler = compositePickler[Message]
    .addConcreteType[Code]
    .addConcreteType[Console]
    .addConcreteType[Chart]
/*

  implicit val devicePickler = generatePickler[Device]
  implicit val valuePickler = generatePickler[DeviceData]
  implicit val samplePickler = generatePickler[Sample]
  implicit val lastMeasurementsPickler = generatePickler[LastMeasurements]

  // implicit val measurementPickler = generatePickler[Measurement]


  implicit val messagesPickler = compositePickler[LambdaMessages.LambdaMessage]
    .addConcreteType[Discover]
    .addConcreteType[Discovered]
    .addConcreteType[SelectDevice]
    .addConcreteType[LastMeasurements]
*/

}

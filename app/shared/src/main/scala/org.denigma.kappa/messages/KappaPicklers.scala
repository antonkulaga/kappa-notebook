package org.denigma.kappa.messages

import boopickle.Default._
import org.denigma.controls.charts.{LineStyles, Point, Series}
import org.denigma.kappa.notebook.parsers.ChartParser
import scala.collection.immutable._
/**
  * Created by antonkulaga on 09/03/16.
  */
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

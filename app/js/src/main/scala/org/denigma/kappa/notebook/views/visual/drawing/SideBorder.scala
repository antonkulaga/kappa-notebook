package org.denigma.kappa.notebook.views.visual.drawing

import org.scalajs.dom

case class SideBorder[T](
                          top: List[(T, Rectangle)],
                          bottom: List[(T, Rectangle)],
                          left: List[(T, Rectangle)] = List.empty,
                          right: List[(T, Rectangle)] = List.empty)
{

}



object SideBorder {

  private def div2[T](sides: List[(T, Rectangle)]) = Math.round(sides.size / 2.0).toInt

  private def splitByWidth[T](sides: List[(T, Rectangle)], w: Double, padding: Double): (List[(T, Rectangle)], List[(T, Rectangle)], Double) =
  {
    val (t, r, s) = sides.foldLeft((List.empty[(T, Rectangle)], List.empty[(T, Rectangle)], 0.0)) {
      case ( (taken, rest, sm), (side, re)) =>
        if( (sm + padding * 2) > w) (taken, (side, re)::rest, sm)
        else {
          val curSm = sm + padding * 2 + re.width
          ((side, re)::taken, rest, sm)
        }
    }
    (t.reverse, r.reverse, s) //in foldLeft we collected them in reverse way

  }

  def extract[T](parentWidth: Double, sides: List[(T, Rectangle)], totalWidth: Double, padding: Double) = totalWidth match {

    case w if w <= parentWidth * 2 =>
      val (top, bottom, sm) = splitByWidth(sides, parentWidth, padding)
      SideBorder(top, bottom)

    case w =>
      val (top, list, _) = splitByWidth(sides, parentWidth, padding)
      list match {

        case Nil =>  SideBorder(top, Nil) //if right is left better to add it to the bottom

        case right::Nil => SideBorder(top, List(right))

        case right::smth::Nil => SideBorder(top, List(right), List(smth))

        case right::other =>

          splitByWidth(other, parentWidth, padding) match {
            case (bottom, Nil, _) => SideBorder(top, List(right), bottom)
            case (bottom, left::Nil, _) => SideBorder(top, List(right), bottom, List(left))
            case (bottom, left, sm) =>
              dom.console.error(s"an agent has so many sides that I cannot visualize it!" +
                s"sides are [${sides.map(_._1).mkString(" ")}]")
              val (btm, last) = left.splitAt(left.size -1)
              SideBorder(top, List(right), bottom++btm, last)
          }
      }

  }
}

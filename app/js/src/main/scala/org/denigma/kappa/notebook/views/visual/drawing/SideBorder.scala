package org.denigma.kappa.notebook.views.visual.drawing

import org.scalajs.dom

case class SideBorder[T](
                          left: List[(T, Rectangle)],
                          top: List[(T, Rectangle)] = Nil,
                          right: List[(T, Rectangle)] = Nil,
                          bottom: List[(T, Rectangle)] = Nil)
{

}



object SideBorder {

  def empty = SideBorder(Nil, Nil, Nil, Nil)

  private def div2[T](sides: List[(T, Rectangle)]) = Math.round(sides.size / 2.0).toInt

  private def splitByWidth[T](sides: List[(T, Rectangle)], w: Double, padding: Double): (List[(T, Rectangle)], List[(T, Rectangle)], Double) =
  {
    val (t, r, s) = sides.foldLeft(
      (
        List.empty[(T, Rectangle)],
        List.empty[(T, Rectangle)],
        0.0)
      ) {
      case (
        (taken, rest, sm),
        (side, re)
        ) =>
        val curSm = sm + padding * 2 + re.width
        if( (sm + padding * 2) > w)
          (taken, (side, re)::rest, curSm)
        else {

          ((side, re)::taken, rest, curSm)
        }
    }
    (t.reverse, r.reverse, s) //in foldLeft we collected them in reverse way

  }

  def extract[T](parentWidth: Double, sides: List[(T, Rectangle)], totalWidth: Double, padding: Double) = sides match {

    case Nil => SideBorder.empty

    case left::Nil => SideBorder(List(left))

    case left::rest =>

      val (top, list, _) = splitByWidth(rest, parentWidth, padding)

      list match {

        case Nil =>  SideBorder(List(left), top, Nil) //if right is left better to add it to the bottom

        case right::Nil => SideBorder(List(left), top, List(right))

        case right::other =>

          splitByWidth(other, parentWidth, padding) match {
            case (bottom, Nil, _) => SideBorder(List(left), top, List(right), bottom)
            case (bottom, extra, sm) =>
              dom.console.error(s"an agent has so many sides that I cannot visualize it!" +
                s"sides are [${sides.map(_._1).mkString(" ")}] , extra are [${extra.map(_._1).mkString(" ")}]")
              //val (btm, last) = left.splitAt(left.size -1)
              SideBorder(List(left), top, List(right), bottom)
              //SideBorder(top, List(right), bottom++btm, last)
          }
      }

  }
}

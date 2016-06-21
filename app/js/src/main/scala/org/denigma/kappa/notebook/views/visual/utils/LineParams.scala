package org.denigma.kappa.notebook.views.visual.utils

import scala.util.Random

object Defs extends Randomizable
{

  val colors = List("green","red","blue",/*"orange",*/"purple","teal")

  val colorMap: Map[String, Int] = Map("green"-> 0xA1CF64, "red"-> 0xD95C5C,"blue" -> 0x6ECFF5,/*"orange" ->0xF05940,*/"purple"-> 0x564F8A, "teal"-> 0x00B5AD)

  def randColorName: String = colors(Random.nextInt(colors.size))

  def colorName: String = randColorName

  def color: Int = colorMap(colorName)

  def headLength = 0// 30

  def headWidth= 0//15


}

case class LineParams(lineColor: Int = Defs.color, headLength: Double = Defs.headLength, headWidth: Double = Defs.headWidth) {

  def hexColor = Integer.toHexString(lineColor)
  // def hex = lineColor.toInt
}

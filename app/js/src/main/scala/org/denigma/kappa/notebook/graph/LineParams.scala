package org.denigma.kappa.notebook.graph

import scala.util.Random

object Colors {
  val green = 0xA1CF64
  val red = 0xD95C5C
  val blue = 0x6ECFF5
  val orange = 0xF05940
  val purple = 0x564F8A
  val teal = 0x00B5AD
}

object Defs extends Randomizable
{


  val colors = List("green","red","blue",/*"orange",*/"purple","teal")

  val colorMap: Map[String, Int] = Map("green"-> Colors.green, "red"-> Colors.red,"blue" -> Colors.blue,/*"orange" ->0xF05940,*/"purple"-> Colors.purple, "teal"-> Colors.teal)

  def randColorName: String = colors(Random.nextInt(colors.size))

  def colorName: String = randColorName

  def color: Int = colorMap(colorName)

  def headLength = 0// 30

  def headWidth= 0//15


}

case class LineParams(lineColor: Int = Defs.color, headLength: Double = Defs.headLength, headWidth: Double = Defs.headWidth, thickness: Double = 3) {

  def hexColor = Integer.toHexString(lineColor)
  // def hex = lineColor.toInt
}

package org.denigma.kappa.syntax

import java.io.InputStream

import ammonite.ops
import ammonite.ops._

import scala.io.Source

trait KappaAgent

case class KappaResult(command: CommandResult, series: List[String])
{
  lazy val ouput: Stream[String] = command.output
}


object Kappa {

  lazy val folder = "/home/antonkulaga/CRI/compbio2/"

  lazy val file = folder+"KaSim"

  var outPutFolder = folder

  lazy val abc = {
    val stream : InputStream = getClass.getResourceAsStream("/examples/abc.ka")
    scala.io.Source.fromInputStream( stream ).getLines.toList
  }

  implicit var wd = ops.Path(new java.io.File(folder))

  def writeFile(name: String, strings: Seq[String]) = {
    val path = wd/ name
    rm! path
    write( path, strings)
    path.toString()
  }


  /**
    * With the signature of A defined in the previous section, the line
'A dimerization'
A(x),A(y ̃p) → A(x!1),A(y ̃p!1) @ γ
denotes a dimerization rule between two instances of agent A provided the second is phos-
phorylated (say that is here the meaning of p) on site y. Note that the bond between both
As is denoted by the identifier !1 which uses an arbitrary integer (!0 would denote the same
bond). In Kappa, a bond may connect exactly 2 sites so any occurrence of a bond identifier
!n has to be paired with exactly one other sibling in the expression.
    */
  def run(name: String, strings: Seq[String], events: Int = 1000000, points: Int = 1000): KappaResult= {
    val kaname = if(name.endsWith(".ka")) name else name+".ka"
    val chart = kaname.replace(".ka", ".out")
    writeFile(kaname, strings)
    //%%KaSim(s"-i $kaname -e $points -o ${kaname.replace(".ka",".out")}")
    val command = %%KaSim("-i", kaname, "-e",events,"-p", points, "-o", chart, "-d",outPutFolder)
    val ch: List[String] = Source.fromFile(folder + chart).getLines().toList
    KappaResult(command, ch)
 }

  def run(strings: Seq[String]): KappaResult = run("tmp.ka", strings)

}

package org.denigma.kappa

import java.io.InputStream

object KappaRes extends KappaRes

trait KappaRes {
  def read(res: String): Iterator[String] = {
    val stream : InputStream = getClass.getResourceAsStream(res)
    scala.io.Source.fromInputStream( stream ).getLines
  }

  def readString(res: String): String = read(res).reduce(_ + "\n" + _)

  lazy val abc = readString("/abc.ka")
}
/**
  * Created by antonkulaga on 31/03/16.
  */
class BasicKappaSuite extends BasicSuite with KappaRes
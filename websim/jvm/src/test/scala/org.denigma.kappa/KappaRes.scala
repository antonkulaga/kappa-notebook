package org.denigma.kappa

import java.io.InputStream

import org.denigma.kappa.messages.RunModel

/**
  * Created by antonkulaga on 04/04/16.
  */
object KappaRes extends KappaRes
trait KappaRes {
  def read(res: String): Iterator[String] = {
    val stream : InputStream = getClass.getResourceAsStream(res)
    scala.io.Source.fromInputStream( stream ).getLines
  }

  def readString(res: String): String = read(res).reduce(_ + "\n" + _)

  lazy val abc = readString("/abc.ka")

  lazy val abcFlow = readString("/abc-cflow.ka")

  lazy val runABCShort: RunModel = messages.RunModel(abc, Some(1000), max_events = Some(10000))

  //lazy val runABCLong: RunModel = WebSim.RunModel(abc, 1000, max_events = Some(1000000))

}
package org.denigma.kappa.syntax

import ammonite.ops._

/**
  * Created by antonkulaga on 11/6/15.
  */
object Tester extends scala.App{
  import Kappa._
  //Kappa.run("hello",a)
  %%KaSim("-i model.ka -e 1000000 -p 1000 -o model.out")

}

import java.io.InputStream

import org.denigma.kappa.notebook._
import org.denigma.kappa.syntax._
import ammonite.ops._
import Kappa._
val a = Kappa.abc
val KappaResult(com,lines) = Kappa.run("abc",a)
println(lines.mkString("\n"))

package org.denigma.kappa.notebook.pages

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.{Directives, Route}
import scalacss.Defaults._

object MyStyles extends StyleSheet.Standalone {
  import dsl._

  ".CodeMirror" -(
    height.auto important
    // width.auto important
    )
  ".CodeMirror-scroll" -(
    overflow.visible,
    height.auto
    )//-(overflowX.auto,overflowY.hidden)


}


class Head extends Directives
{

  lazy val webjarsPrefix = "lib"
  lazy val resourcePrefix = "resources"

  def mystyles =    path("styles" / "mystyles.css"){
    complete  {
      HttpResponse(  entity = HttpEntity(MediaTypes.`text/css`,  MyStyles.render   ))   }
  }

  def loadResources = pathPrefix(resourcePrefix~Slash) {
    getFromResourceDirectory("")
  }


  def webjars =pathPrefix(webjarsPrefix ~ Slash)  {  getFromResourceDirectory(webjarsPrefix)  }

  def routes: Route = mystyles ~ webjars ~ loadResources
}

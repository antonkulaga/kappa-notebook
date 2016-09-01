package org.denigma.kappa.notebook.pages

import akka.http.extensions.pjax.PJax
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.{Route, Directives}
import org.denigma.controls.Twirl
import play.twirl.api.Html

class Pages extends Directives with PJax{

  def defaultPage: Option[Html] = {
    Some(html.notebook())
  }

  def index: Route =  pathSingleSlash{ ctx=>
    ctx.complete {
      HttpResponse(  entity = HttpEntity(MediaTypes.`text/html`.withCharset(HttpCharsets.`UTF-8`), html.index(defaultPage).body  ))
    }
  }

  def page: Route =  path("page") { ctx=>
    ctx.complete {
      val code = editor.html.index()
      val pg = loadPage(code)
      HttpResponse(  entity = HttpEntity(MediaTypes.`text/html`.withCharset(HttpCharsets.`UTF-8`), pg.body  ))
    }
  }

  lazy val loadPage: Html => Html = h => html.index(Some(h))

  def test: Route = path("test") { ctx=>
    ctx.complete {
      HttpResponse(  entity = HttpEntity(MediaTypes.`text/html`.withCharset(HttpCharsets.`UTF-8`), html.test().body  ))
    }
  }

  def notFound: Route = pathPrefix("test" ~ Slash) { ctx=>
      pjax[Twirl](Html(s"<h1>${ctx.unmatchedPath}</h1>"),loadPage){h=> c=>
        val resp = HttpResponse(  entity = HttpEntity(MediaTypes.`text/html`.withCharset(HttpCharsets.`UTF-8`), h.body  ))
        c.complete(resp)
      }(ctx)
    }


  def routes: Route = index  ~ page ~ test ~ notFound


}
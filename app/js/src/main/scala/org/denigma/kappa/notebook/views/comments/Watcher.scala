package org.denigma.kappa.notebook.views.comments

import org.denigma.codemirror.Editor
import org.scalajs.dom.html._

/**
  * Created by antonkulaga on 8/28/16.
  */
trait Watcher {

  type Data

  def parse(editor: Editor, lines: List[(Int, String)], currentNum: Int): Unit

}

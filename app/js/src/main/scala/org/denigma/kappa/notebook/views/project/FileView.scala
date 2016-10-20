package org.denigma.kappa.notebook.views.project

import org.denigma.binding.binders.Events
import org.denigma.binding.views.BindableView
import org.denigma.kappa.messages._
import org.denigma.kappa.notebook.views.common.SimpleFileView
import org.scalajs.dom
import org.scalajs.dom._
import org.scalajs.dom.raw.Element
import rx.Ctx.Owner.Unsafe.Unsafe
import rx.Rx.Dynamic
import rx._
import org.denigma.binding.extensions._
import scala.collection.immutable._

class FileView(elem: Element,
               file: Var[KappaFile],
               val saveRequest: Var[FileRequests.Save],
               val removeRequest: Var[FileRequests.Remove],
               openFile: Var[KappaFile]) extends SimpleFileView(elem, file){


  val isSource: Rx[Boolean] = fileType.map(f=>f==FileType.source)
  /*
  val isImage: Rx[Boolean] = fileType.map(f=>f==FileType.image)
  val isVideo: Rx[Boolean] = fileType.map(f=>f==FileType.video)
  val isPaper: Rx[Boolean] = fileType.map(f=>f==FileType.pdf)
  */

  val runnable: Dynamic[Boolean] = Rx{
    isSource() && file().active
  }

  val removeClick: Var[MouseEvent] = Var(Events.createMouseEvent())
  removeClick.triggerLater{
    val message = s"Do you really want to remove '${name.now}' file?"
    val confirmation = window.confirm(message)
    if(confirmation) removeRequest() = FileRequests.Remove(Set(path.now))
  }

  val saveClick: Var[MouseEvent] = Var(Events.createMouseEvent())
  saveClick.triggerLater{
   saveRequest() =  FileRequests.Save( List(file.now), rewrite = true)
  }

  val renameClick: Var[MouseEvent] = Var(Events.createMouseEvent())
  renameClick.triggerLater{

  }

  val gotoClick: Var[MouseEvent] = Var(Events.createMouseEvent())
  gotoClick.triggerLater{
    if(!editable.now) openFile() = file.now
  }

  val downloadClick: Var[MouseEvent] = Var(Events.createMouseEvent())
  downloadClick.triggerLater{
    val url: String = "http://"+dom.window.location.host +"/files/" + path.now
    dom.window.open(url, "_blank")
  }

  val fileClick: Var[MouseEvent] = Var(Events.createMouseEvent())
  fileClick.onChange(fileClickHandler)

  protected def fileClickHandler(event: MouseEvent) = {
    if(!editable.now) openFile() = file.now
  }


}

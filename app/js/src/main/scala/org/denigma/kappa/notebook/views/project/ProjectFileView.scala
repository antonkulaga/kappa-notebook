package org.denigma.kappa.notebook.views.project
import org.denigma.binding.binders.Events
import org.denigma.binding.views.BindableView
import org.denigma.kappa.messages._
import org.denigma.kappa.notebook.views.actions.Movements
import org.scalajs.dom._
import org.scalajs.dom.raw.Element
import rx.Ctx.Owner.Unsafe.Unsafe
import rx.Rx.Dynamic
import rx._

import scala.collection.immutable._

class ProjectFileView(val elem: Element, val file: KappaFile, parentName: Rx[String], input: Var[KappaMessage], output: Var[KappaMessage]) extends BindableView {

  val editable = Var(false)

  val name: Rx[String] = Var(file.name)
  val fileType: Rx[FileType.Value] = Var(file.fileType)

  val isSource: Rx[Boolean] = fileType.map(f=>f==FileType.source)
  val isImage: Rx[Boolean] = fileType.map(f=>f==FileType.image)
  val isVideo: Rx[Boolean] = fileType.map(f=>f==FileType.video)
  val isPaper: Rx[Boolean] = fileType.map(f=>f==FileType.pdf)

  val runnable: Dynamic[Boolean] = Rx{
    isSource() && file.active
  }

  val saved = Var(file.saved)

  val icon: Rx[String] = fileType.map{
    case FileType.pdf => "File Pdf Outline" + " large icon"
    case FileType.txt => "File Text Outline" + " large icon"
    case FileType.source => "File Code Outline" + " large icon"
    case FileType.image => "File Image Outline" + " large icon"
    case FileType.video => "File Video Outline" + " large icon"
    case other => "File Outline"
  }

  val fileClick: Var[MouseEvent] = Var(Events.createMouseEvent())
  fileClick.triggerLater{
    if(!editable.now) goToFile()
  }

  protected def goToFile() = {
    input() = Movements.toFile(file)
    /*
    fileType.now match {
      case FileType.pdf => input() =
        KappaMessage.Container()
          .andThen(Go.ToTab(MainTabs.Papers))
          .andThen(GoToPaper(Bookmark(file.name, 1)))

      case FileType.source => input() =
        KappaMessage.Container()
          .andThen(Go.ToTab(MainTabs.Editor))
          .andThen(Go.ToSource(filename = file.name))

      case FileType.image=> input() =
        KappaMessage.Container()
          .andThen(Go.ToTab(MainTabs.Figures))
          .andThen(GoToFigure(file.name))


      case other => //do nothing
    }
    */
  }

  val removeClick: Var[MouseEvent] = Var(Events.createMouseEvent())
  removeClick.triggerLater{
    val message = s"Do you really want to remove '${file.name}' file?"
    val confirmation = window.confirm(message)
    if(confirmation) output() = FileRequests.Remove(parentName.now, file.name)
  }

  val saveClick: Var[MouseEvent] = Var(Events.createMouseEvent())
  saveClick.triggerLater{
    val saveRequest = FileRequests.Save(projectName = parentName.now, List(file), rewrite = true)
    //println("save request")
    //pprint.pprintln(saveRequest)
    output() = saveRequest
  }

  val renameClick: Var[MouseEvent] = Var(Events.createMouseEvent())
  renameClick.triggerLater{

  }


}

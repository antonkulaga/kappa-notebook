package org.denigma.kappa.notebook.views.common

import org.denigma.binding.binders.Events
import org.denigma.binding.views.BindableView
import org.denigma.kappa.messages._
import org.scalajs.dom
import org.scalajs.dom._
import org.scalajs.dom.raw.Element
import rx.Ctx.Owner.Unsafe.Unsafe
import rx.Rx.Dynamic
import rx._
import org.denigma.binding.extensions._

import scala.collection.immutable._

class SimpleFileView(val elem: Element,
                     val file: Var[KappaFile]) extends BindableView {

  val editable = Var(false)

  val name: Rx[String] = file.map(f=>f.name)
  val path: Rx[String] = file.map(f=>f.path)
  val fileType: Rx[FileType.Value] = file.map(f=>f.fileType)

  val saved = file.map(f=>f.saved)

  lazy val icon: Rx[String] = fileType.map{
    case FileType.pdf => "File Pdf Outline" + " large icon"
    case FileType.txt => "File Text Outline" + " large icon"
    case FileType.source => "File Code Outline" + " large icon"
    case FileType.image => "File Image Outline" + " large icon"
    case FileType.video => "File Video Outline" + " large icon"
    case other => "File Outline"
  }

}


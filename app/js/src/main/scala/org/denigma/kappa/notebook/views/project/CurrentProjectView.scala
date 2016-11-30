package org.denigma.kappa.notebook.views.project

import java.nio.ByteBuffer

import org.denigma.binding.binders.{Events, GeneralBinder}
import org.denigma.binding.extensions._
import org.denigma.binding.views.{CollectionMapView, CollectionSortedSetView}
import org.denigma.kappa.messages._
import org.denigma.kappa.notebook.circuits.CurrentProjectCircuit
import org.scalajs.dom
import org.scalajs.dom._
import org.scalajs.dom.html.Input
import org.scalajs.dom.raw.Element
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

import scala.collection.immutable._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.typedarray.{ArrayBuffer, Int8Array, TypedArray, TypedArrayBuffer}
import scala.util.{Failure, Success, Try}

/**
  * The view responsible for rendering current project files
  * @param elem HTML element to bind to
  * @param circuit (viewmodel)
  * @param uploadId id that is used for file uploading
  */
class CurrentProjectView(val elem: Element,
                         val circuit: CurrentProjectCircuit,
                         val uploadId: String
                        ) extends CollectionMapView {

  val saveRequest = circuit.outgoingPortFrom(FileRequests.Save.empty)

  val removeRequest = circuit.outgoingPortFrom(FileRequests.Remove.empty)

  val openFile = circuit.intoIncomingPort[KappaFile, KappaMessage](KappaSourceFile.empty, skipFirst = true){
    f=>
      val message = Go.ToFile(f)
      //println(s"to file name ${f.path}")
      message
  }

  val goToFile = circuit.intoIncomingPort[KappaFile, KappaMessage](KappaSourceFile.empty, skipFirst = true){
    f=>
      val message = Go.ToFile(f)
      println(s"to file name ${f.path}")
      Animate(message, false)
  }

  lazy val currentProject = circuit.currentProject


  override type Key = String

  override type Value = KappaFile

  lazy val items: Rx[Map[String, KappaFile]] = currentProject.map(proj => proj.folder.allFilesMap)

  lazy val projectName = currentProject.map(_.name)

  lazy val uploadInput = sq.byId(uploadId).get.asInstanceOf[Input]

  override type ItemView = FileView

  val newFileName: Var[String] = Var("")

  val hasNewFile: Rx[Boolean] = newFileName.map(f=>f.length > 1)

  val fileName: Rx[String] = newFileName.map{
    case f if f.endsWith(".ka")=>f
    case other => other
  }

  val canCreate: Rx[Boolean] = Rx{
    val proj = currentProject()
    val path = proj + "/" + fileName()
    !currentProject().folder.files.exists(f => path ==f.path)
  }

  val hasUnsaved: Rx[Boolean] = circuit.unsaved.map(u=>u.nonEmpty)

  val addFile = Var(Events.createMouseEvent())
  addFile.triggerIf(canCreate){ ev=>
    val file = KappaSourceFile(projectName.now + "/" + newFileName.now, "", saved = false)
    saveRequest()= FileRequests.Save(List(file), rewrite = false, getSaved = true)
  }

  val uploadFile = Var(Events.createMouseEvent())
  uploadFile.triggerLater{
    uploadInput.click()
  }

  val saveAll = Var(Events.createMouseEvent())
  saveAll.triggerLater{
    circuit.saveAll()
   }

/*
  override def newItemView(item: Item): ItemView = constructItemView(item){
    case (el, _) => new FileView(el, item, saveRequest, removeRequest, openFile).withBinder(v => new GeneralBinder(v))
  }
*/


  override def updateView(view: FileView, key: String, old: KappaFile, current: KappaFile): Unit = {
    view.file() = current
  }

  override def newItemView(key: String, value: KappaFile): FileView = this.constructItemView(key){
    case (el, _) =>
      new FileView(el, Var(value), saveRequest, removeRequest, openFile, goToFile).withBinder(v => new GeneralBinder(v))
  }

  val save = Var(Events.createMouseEvent())
  save.onChange{ ev =>
      circuit.output() = ProjectRequests.Save(currentProject.now)
      //connector.send(Save(currentProject.now))
  }

  val download: Var[MouseEvent] = Var(Events.createMouseEvent())
  download.triggerLater{
      val mes = ProjectRequests.Download(currentProject.now.name)
      //println("download ="+mes)
      circuit.output() = mes
  }

  override def subscribeUpdates() = {
    super.subscribeUpdates()
    uploadInput.addEventListener[Event](Events.change, filesHandler _)
  }

  protected def filesHandler(event: org.scalajs.dom.Event) = {
    val name = fileName.now
    dom.console.info("FILE UPLOAD WORKS!")
    val files = uploadInput.files.toList
    dom.console.info("FIELS ARE :"+files)
    files.foreach{ f =>
      f.readAsArrayBuffer.onComplete{
        case Failure(th) => dom.console.error("file upload failed with: "+th)
        case Success(result) =>
          val array = new Int8Array(result).toArray
          val fl = KappaBinaryFile(projectName.now+"/"+f.name, array)
          saveRequest() = FileRequests.Save(List(fl), rewrite = true, getSaved = true)
      }
      /*
      f.readAsByteBuffer.onComplete{
          case Failure(th) => dom.console.error("file upload failed with: "+th)
          case Success(result) =>
            val fl = KappaBinaryFile(projectName.now+"/"+f.name, result)
            saveRequest() = FileRequests.Save(List(fl), rewrite = true, getSaved = true)
        }
        */
    }
    //val toUplod = FileRequests.UploadBinary()
    //val k = KappaFile("", name, "", saved = false)
    //val uploadRequest = FileRequests.Save(projectName.now, List(k), rewrite = false, getSaved = true)
    //output() = uploadRequest
  }
}



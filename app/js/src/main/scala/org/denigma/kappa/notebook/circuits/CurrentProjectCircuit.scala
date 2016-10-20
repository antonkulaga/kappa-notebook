package org.denigma.kappa.notebook.circuits

import org.denigma.binding.extensions._
import org.denigma.kappa.messages.{KappaMessage, KappaProject, _}
import org.denigma.kappa.notebook.actions.Commands
import org.scalajs.dom
import org.scalajs.dom._
import org.scalajs.dom.raw.BlobPropertyBag
import rx.Ctx.Owner.Unsafe.Unsafe
import rx.{Var, _}

import scala.collection.immutable._
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.typedarray.Uint8Array

/**
  * Circuit that is responsible for CurrentProject view logic
  * @param input input messages Rx
  * @param output is used to send messages to the server
  * @param currentProject
  */
class CurrentProjectCircuit(input: Var[KappaMessage],
                            output: Var[KappaMessage],
                            val currentProject: Var[KappaProject]) extends Circuit(input, output){

  val unsaved: Rx[Map[String, KappaFile]] = currentProject.map(p => p.folder.allFiles.filterNot(f => f.saved).map(f => f.path -> f).toMap)

  protected def inProject(str: String) = str.startsWith(currentProject.now.name)|| str.startsWith("/"+currentProject.now.name)

  override protected def onInputMessage(message: KappaMessage): Unit = message match {

    case d @ FileResponses.Downloaded(label, data)=>
      val name = if(!label.contains(".")) label+".zip" else label
      //println("DOWNLOADED IS = "+d)
      if(d != FileResponses.Downloaded.empty) saveBinaryAs(name, data)

    case b @ KappaBinaryFile(path, buff, _, _) if currentProject.now.folder.allFilesMap.contains(path) =>
      //currentProject.now = currentProject.now.copy(folder = )
      dom.console.log("updating binary parts is not yet implemented")

    case Done(FileRequests.Remove(pathes), _)  =>
      val (in, not) = pathes.partition(inProject)
      if(in.nonEmpty){
        currentProject() = currentProject.now.copy(folder = currentProject.now.folder.removeFiles(pathes))
      }

    case resp @ FileResponses.RenamingResult(renamed: Map[String, (String, String)], nameConflicts, notFound)=>
      dom.console.error("RENAMING IS NOT YET IMPLEMENTED IN UI")

    case resp @ FileResponses.SavedFiles(Left(pathes)) =>
      val proj = currentProject.now
      currentProject() = { proj.copy(folder = proj.folder.markSaved(pathes)) }

    case resp @ FileResponses.SavedFiles(Right(files)) =>
      val proj = currentProject.now
      currentProject() = proj.copy(folder = proj.folder.addFiles(files))

    case Commands.SaveAll =>
      saveAll()

    case _=> //do nothing

  }

  def saveAll() = {
    val toSave = unsaved.now.values.toList
    if(toSave.nonEmpty) {
      output() = FileRequests.Save(toSave, rewrite = true)
    }
  }

  protected def saveBinaryAs(filename: String, data: Array[Byte]) = {
    //println(s"binary for $filename")
    val pom = dom.document.createElement("a")
    pom.setAttribute("id","pom")
    val options = BlobPropertyBag("octet/stream")
    val arr = new Uint8Array(data.toJSArray)
    val blob = new Blob(js.Array(arr), options)
    //val url = dom.window.dyn.URL.createObjectURL(blob)
    val reader = new FileReader()
    def onLoadEnd(ev: ProgressEvent): Any = {
      val url = reader.result
      pom.asInstanceOf[dom.raw.HTMLAnchorElement].href = url.asInstanceOf[String]
      pom.setAttribute("download", filename)
      pom.dyn.click()
      if(pom.parentNode==dom.document) dom.document.removeChild(pom)
    }
    reader.onloadend = onLoadEnd _
    reader.readAsDataURL(blob)
  }

}

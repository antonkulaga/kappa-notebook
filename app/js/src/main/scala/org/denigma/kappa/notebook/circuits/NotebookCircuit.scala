package org.denigma.kappa.notebook.circuits

import org.denigma.kappa.messages._
import org.denigma.kappa.notebook.actions.{Commands, Animations}
import org.scalajs.dom
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe
import org.denigma.binding.extensions._

import scala.collection.immutable.{Map, SortedMap}

class NotebookCircuit(input: Var[KappaMessage], output: Var[KappaMessage]) extends Circuit(input, output){

  val currentProject: Var[KappaProject] = Var(KappaProject.default)

  val selectedProject: Var[KappaProject] = Var(KappaProject.default)

  selectedProject.onChange{ proj => output() = ProjectRequests.Load(proj) }

  val allProjects = Var(SortedMap.empty[String, KappaProject])

  protected def goMessage(messages: List[KappaMessage], delay: Int): Unit = messages match {
    case Nil =>
    case head::tail =>
      input() = head
      scalajs.js.timers.setTimeout(delay)(goMessage(tail, delay))
  }

  protected def onInputMessage(message: KappaMessage): Unit = message match {

    case KappaMessage.Container(messages, 0) =>

      messages.foreach(mess=> input() = mess) //flatmapping

    case KappaMessage.Container(messages, delay) =>

      goMessage(messages, delay)

    case ProjectResponses.LoadedProject(proj) =>
      //println("LOADED PROJECT IS")
      //pprint.log(proj)
      currentProject() = proj
      selectedProject() = proj

    case ProjectResponses.ProjectList(projects) =>
      allProjects() = SortedMap.empty[String, KappaProject] ++ projects.map(p=> (p.name, p))

    case org.denigma.kappa.messages.Done(cr: ProjectRequests.Create, _) =>
      println("project has been created, loading it...")
      output() = ProjectRequests.Load(cr.project)

    case SourceUpdate(from, to) =>
      val proj = currentProject.now
      currentProject() = proj.copy(folder = proj.folder.addFiles(List(to)))

    case _=> //do nothing
  }

}


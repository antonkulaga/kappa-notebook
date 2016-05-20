package org.denigma.kappa.notebook.views

import org.denigma.binding.views._
import org.denigma.codemirror.{Editor, PositionLike}
import org.denigma.controls.code.CodeBinder
import org.denigma.controls.papers.Bookmark
import org.denigma.kappa.messages._
import org.denigma.kappa.notebook.{Selector, WebSocketTransport}
import org.denigma.kappa.notebook.views.annotations.{ImagesView, VideosView}
import org.denigma.kappa.notebook.views.editor.EditorUpdates
import org.denigma.kappa.notebook.views.annotations.papers.PapersView
import org.denigma.kappa.notebook.views.simulations.SimulationsView
import org.scalajs.dom.raw.Element
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe
import org.denigma.binding.extensions._
import rx.Rx.Dynamic

import scala.collection.immutable.SortedSet

class TabsView(
               val elem: Element,
               val connector: WebSocketTransport,
               val selector: Selector,
               val loaded: Rx[ProjectResponses.Loaded],
               val kappaCursor: Var[Option[(Editor, PositionLike)]],
               val sourceMap: Rx[Map[String, KappaFile]]
              ) extends BindableView {

  self =>

  //val currentProject =  loaded.map(_.projectOpt.getOrElse(KappaProject.default))


  val papers: Var[Map[String, Bookmark]] = Var(Map.empty)

  val images: Var[Map[String, String]] = Var(Map.empty)

  val videos: Var[Map[String, String]] = Var(Map.empty)


  val currentProjOpt: Rx[Option[KappaProject]] = loaded.map(_.projectOpt)

  currentProjOpt.onChange{
    case Some(proj)=>
      images() = proj.images.map{
        case i=>
          val p = proj.name + "/" +i.name
          i.name -> p
        }.toMap

      videos() = proj.videos.map(i=> i.name -> i.path).toMap

      papers() = proj.papers.map{case
        p=>
        println("proj folder path = "+proj.folder.path)
        //val n = p.name.replace(proj.folder.path, "")
        p.name -> Bookmark(p.path, 1)
      }.toMap

    case None =>
  }

  val simulations: Var[Map[(Int, RunModel), SimulationStatus]] = Var(Map.empty[(Int, RunModel), SimulationStatus])

  val launcher: Var[LaunchModel] = Var( LaunchModel("", RunModel(code = "", max_events = Some(10000), max_time = None)))

  lazy val selected = selector.tab

  protected def concat() = {
    sourceMap.now.values.foldLeft(""){
      case (acc, e)=> acc + "\n"+ e.content
    }
  }

  override def bindView() = {
    super.bindView()
    import com.softwaremill.quicklens._
    launcher.triggerLater{
      val l: LaunchModel = launcher.now
      val launchParams = l.modify(_.parameters).setTo(l.parameters.copy(code = concat()))
      //println("PARAMS TO THE SERVER = "+launchParams)
      connector.output() = launchParams
    }
    connector.input.foreach{
      case SimulationResult(server, status, token, params) =>
        println("percent: "+ status.percentage)
        simulations() = simulations.now.updated((token, params.getOrElse(status.runParameters)), status)
        //if(errors.now.nonEmpty) errors() = List.empty
      case other => //do nothing
    }
  }


  protected def defaultContent = ""
  protected def defaultLabel = ""

  type Item = Rx[common.TabItem]



  val editorsUpdates: Var[EditorUpdates] = Var(EditorUpdates.empty)

  override lazy val injector = defaultInjector
    .register("Simulations") {
      case (el, params) =>
        new SimulationsView(el, selected, simulations, launcher).withBinder(new CodeBinder(_))
    }
    /*
    .register("Console") {
      case (el, params) =>
        new ConsoleView(el, hub.console, selected).withBinder(new CodeBinder(_))
    }
    */

    .register("Images") {
      case (el, params) =>
        new ImagesView(el, selected, selector, images).withBinder(new CodeBinder(_))
    }
    .register("Videos") {
      case (el, params) =>
        el.id = "Videos"
        new VideosView(el, videos, selector.video).withBinder(new CodeBinder(_))
    }
    .register("Papers") {
      case (el, params) =>
        new PapersView(el, loaded.map(l=>l.projectOpt.map(_.name).getOrElse("")), connector, selected, papers, selector, kappaCursor).withBinder(new CodeBinder(_))
    }
    .register("UnderDevelopment") {
      case (el, params) =>
        el.id = "UnderDevelopment" //dirty trick to set viewid
        new UnderDevelopment(el, selected).withBinder(new CodeBinder(_))
    }
    /*
    .register("SBOLEditor") {
      case (el, params) =>
        new SBOLEditor(el, hub, selected, editorsUpdates).withBinder(new CodeBinder(_))
    }
    */
}
class UnderDevelopment(val elem: Element, val selected: Var[String]) extends BindableView with common.TabItem {

}
package org.denigma.kappa.notebook.views


import org.denigma.binding.binders.GeneralBinder
import org.denigma.binding.extensions._
import org.denigma.binding.views.BindableView
import org.denigma.codemirror.{Editor, PositionLike}
import org.denigma.controls.code.CodeBinder
import org.denigma.controls.login.Session
import org.denigma.controls.papers.{Bookmark, PaperLoader}
import org.denigma.kappa.messages._
import org.denigma.kappa.notebook.views.annotations.AnnotatorNLP
import org.denigma.kappa.notebook.views.editor.{CommentsWatcher, EditorUpdates, KappaCodeEditor, KappaWatcher}
import org.denigma.kappa.notebook.views.figures.{Image, Video, Figure, FiguresView}
import org.denigma.kappa.notebook.views.papers.{PapersView, WebSocketPaperLoader}
import org.denigma.kappa.notebook.views.project.ProjectsPanelView
import org.denigma.kappa.notebook.views.simulations.SimulationsView
import org.denigma.kappa.notebook.views.visual.drawing.SvgBundle.all._
import org.denigma.kappa.notebook._
import org.scalajs.dom
import org.scalajs.dom.raw.Element
import org.scalajs.dom.svg.SVG
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._
import org.querki.jquery.$

import scala.collection.immutable.SortedSet
import scala.concurrent.duration._
import scala.scalajs.js

class NotebookView(val elem: Element, val session: Session) extends BindableView
{
  self =>

  val connector: WebSocketTransport = WebSocketTransport("notebook", "guest" + Math.random() * 1000)

  val (input: Var[KappaMessage], output: Var[KappaMessage]) = connector.IO

  val loaded: Var[ProjectResponses.Loaded] = Var(ProjectResponses.Loaded.empty)

  val serverErrors = Var(ServerErrors.empty)

  val kappaServerErrors = Var(KappaServerErrors.empty)

  val path = Var("files")

  val sources: Var[Map[String, KappaFile]] = Var(Map.empty[String, KappaFile])

  val location: Var[Bookmark] = Var(Bookmark("", 1, Nil))

  val figures: Var[Map[String, Figure]] = Var(Map.empty)

  val currentProjOpt: Rx[Option[KappaProject]] = loaded.map(_.projectOpt)

  val currentProjectName = currentProjOpt.map{
    case Some(p) => p.name
    case None => KappaProject.default.name
  }

  currentProjOpt.onChange{
    case Some(proj)=>

      val images = proj.images.map{
        case i=>
          val p = proj.name + "/" +i.name
          i.name ->Image(i.name, p)
      }
      val videos = proj.videos.map(i=> i.name -> Video(i.name , i.path))
      figures() = (images ++ videos).toMap

    case None =>
  }


  val editorsUpdates: Var[EditorUpdates] = Var(EditorUpdates.empty)

  override def bindView() = {
    super.bindView()
    connector.onOpen.triggerLater{
      println("websocket opened")
      val toLoad = ProjectRequests.Load(KappaProject.default)
      connector.output() = toLoad //ask to load default project
    }
    input.foreach(onMessage)
    connector.open()
  }

  protected def onMessage(message: KappaMessage): Unit = message match {

    case KappaMessage.Container(messages) =>
      messages.foreach(mess=>input() = mess) //flatmapping
    case Go.ToTab(tabName) =>
        if(menuMap.now.contains(tabName))
        {
          val value: ViewElement = menuMap.now(tabName)
          value.id match {
            case null => dom.console.error(s"$tabName id is null")
            case undef if js.isUndefined(undef)=> dom.console.error(s"$tabName id is undefined")
            case ident =>
              dom.window.location.hash = ""
              dom.window.location.hash = ident
          }
        }
       else {
        dom.console.error(s"Go.ToTab($tabName) failed as there is not such tab in the menu")
        }
    case ld: ProjectResponses.Loaded =>
      //println("LOQDED = "+ld)
      loaded() = ld
    //case GoToPaper()
    case KappaMessage.ServerResponse(ers: ServerErrors) =>  serverErrors() = ers
    case KappaMessage.ServerResponse(ers: KappaServerErrors) => kappaServerErrors() = ers
    case Failed(operation, ers, username) =>  kappaServerErrors() = kappaServerErrors.now.copy(errors = kappaServerErrors.now.errors ++ ers)

    case other => //do nothing
  }

  val commentManager = new CommentsWatcher(editorsUpdates, location/*, selector*/)

  lazy val s: SVG = {
    val t = svg(/*width :=  defaultWidth, height := defaultHeight*/).render
    t.style.position = "absolute"
    t.style.top = "-9999"
    elem.appendChild(t)
    t
  }

  val kappaCursor: Var[Option[(Editor, PositionLike)]] = Var(None)

  val kappaWatcher: KappaWatcher = new KappaWatcher(kappaCursor, editorsUpdates, s)

  val menu: Var[List[(String, Element)]] = Var(List.empty[(String, Element)])

  val menuMap = menu.map(list=>list.toMap)

  protected def addMenuItem(el: Element, title: String) = {
    menu() = menu.now :+ (title, el)
  }

  override lazy val injector = defaultInjector
    .register("MainMenuView") {
      case (el, args) =>
        new MainMenuView(el, menu).withBinder(n => new FixedBinder(n))
    }
    .register("ProjectsPanel"){
      case (el, args) =>
        val v = new ProjectsPanelView(el, sources, loaded, connector.input, connector.output, kappaWatcher).withBinder(n => new CodeBinder(n))
        addMenuItem(el, MainTabs.Projects)
        v
     }
    .register("KappaEditor"){
      case (el, args) =>
        val editor = new KappaCodeEditor(el, sources, input, kappaCursor, editorsUpdates).withBinder(n => new CodeBinder(n))
        addMenuItem(el, MainTabs.Editor)
        editor
    }
    .register("Simulations") {
      case (el, params) =>
        val v = new SimulationsView(el, sources, input, output).withBinder(new CodeBinder(_))
        addMenuItem(el, MainTabs.Simulations)
        v
    }
    .register("Papers") {
      case (el, params) =>
        val v = new PapersView(el, location, currentProjectName, connector, kappaCursor).withBinder(new CodeBinder(_))
        addMenuItem(el, MainTabs.Papers)
        v
    }
    .register("Annotator"){
      case (el, args) =>
        val v = new AnnotatorNLP(el).withBinder(new GeneralBinder(_))
        addMenuItem(el, MainTabs.Annotations)
        v
    }
    .register("Figures") {
      case (el, params) =>
        val v = new FiguresView(el, figures).withBinder(new CodeBinder(_))
        addMenuItem(el, MainTabs.Figures)
        v
    }
}


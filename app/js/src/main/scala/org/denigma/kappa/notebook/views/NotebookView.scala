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
import org.scalajs.dom.raw.Element
import org.scalajs.dom.svg.SVG
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

import scala.collection.immutable.SortedSet
import scala.concurrent.duration._

class NotebookView(val elem: Element, val session: Session) extends BindableView
{
  self =>

  val connector: WebSocketTransport = WebSocketTransport("notebook", "guest" + Math.random() * 1000)

  val (input: Var[KappaMessage], output: Var[KappaMessage]) = (connector.input, connector.output)

  val loaded: Var[ProjectResponses.Loaded] = Var(ProjectResponses.Loaded.empty)

  val errors = Var(List.empty[String])

  val name = Var("HelloWorld!")

  val path = Var("files")

  val sourceMap: Var[Map[String, KappaFile]] = Var(Map.empty[String, KappaFile])

  val otherFiles = Var(SortedSet.empty[KappaFile])

  val currentProject: Rx[KappaProject] = Rx{
    val sourceFiles = sourceMap().values.toSeq
    val fls = SortedSet(sourceFiles:_*) ++ otherFiles()
    val folder = KappaFolder(path(), files = fls)
    KappaProject(name(), folder)
  }

  val location: Var[Bookmark] = Var(Bookmark("", 1, Nil))

  val selectedFigure: Var[String] = Var("")

  val selectedSource: Var[String] = Var("")

  val figures: Var[Map[String, Figure]] = Var(Map.empty)

  val currentProjOpt: Rx[Option[KappaProject]] = loaded.map(_.projectOpt)

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

  lazy val currentProjectName: Rx[String] = currentProject.map(p=>p.name)

  val editorsUpdates: Var[EditorUpdates] = Var(EditorUpdates.empty)

  override def bindView() = {
    super.bindView()
    loaded.onChange{
      case ld=>
        ld.projectOpt match {
          case Some(proj)=>
            name() = proj.name
            otherFiles() = proj.nonsourceFiles
            val sources = proj.sourceMap
            sourceMap.set(sources)
          case None =>
        }
    }
    connector.onOpen.triggerLater{
      println("websocket opened")
      val toLoad = ProjectRequests.Load(KappaProject.default)
      connector.output() = toLoad //ask to load default project
    }
    input.foreach{
      case ld: ProjectResponses.Loaded =>
        //println("LOQDED = "+ld)
        loaded() = ld
      case SyntaxErrors(server, ers, params) =>  errors() = ers
      case Failed(operation, ers, username) =>  errors() = ers
      case ServerErrors(ers) => errors() = ers
      case other => //do nothing
    }

    connector.open()
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
        val v = new ProjectsPanelView(el, currentProject, loaded, connector.input, connector.output, kappaWatcher).withBinder(n => new CodeBinder(n))
        addMenuItem(el, "Projects")
        v
     }
    .register("KappaEditor"){
      case (el, args) =>
        val editor = new KappaCodeEditor(el, sourceMap, selectedSource, errors, input, kappaCursor, editorsUpdates).withBinder(n => new CodeBinder(n))
        addMenuItem(el, "Editor")
        editor
    }
    .register("Simulations") {
      case (el, params) =>
        val v = new SimulationsView(el, sourceMap, input, output).withBinder(new CodeBinder(_))
        addMenuItem(el, "Simulations")
        v
    }
    .register("Papers") {
      case (el, params) =>
        val v = new PapersView(el, location, currentProjectName, connector, kappaCursor).withBinder(new CodeBinder(_))
        addMenuItem(el, "Papers")
        v
    }
    .register("Annotator"){
      case (el, args) =>
        val v = new AnnotatorNLP(el).withBinder(new GeneralBinder(_))
        addMenuItem(el, "Annotations")
        v
    }
    .register("Figures") {
      case (el, params) =>
        val v = new FiguresView(el, selectedFigure, figures).withBinder(new CodeBinder(_))
        addMenuItem(el, "Figures")
        v
    }
}

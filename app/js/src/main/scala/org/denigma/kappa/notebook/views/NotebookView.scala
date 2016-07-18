package org.denigma.kappa.notebook.views


import org.denigma.binding.binders.GeneralBinder
import org.denigma.binding.extensions._
import org.denigma.binding.views.BindableView
import org.denigma.codemirror.{Editor, PositionLike}
import org.denigma.controls.code.CodeBinder
import org.denigma.controls.login.Session
import org.denigma.controls.papers.Bookmark
import org.denigma.kappa.messages.ServerMessages.{KappaServerErrors, ServerConnection}
import org.denigma.kappa.messages._
import org.denigma.kappa.notebook._
import org.denigma.kappa.notebook.extensions._
import org.denigma.kappa.notebook.views.annotations.AnnotatorNLP
import org.denigma.kappa.notebook.views.common.ServerConnections
import org.denigma.kappa.notebook.views.editor.{CommentsWatcher, EditorUpdates, KappaCodeEditor, KappaWatcher}
import org.denigma.kappa.notebook.views.figures.{Figure, FiguresView}
import org.denigma.kappa.notebook.views.menus.MainMenuView
import org.denigma.kappa.notebook.views.papers.PapersView
import org.denigma.kappa.notebook.views.project.ProjectsPanelView
import org.denigma.kappa.notebook.views.simulations.SimulationsView
import org.denigma.kappa.notebook.views.visual.VisualPanelView
import org.denigma.kappa.notebook.views.visual.rules.drawing.SvgBundle.all._
import org.scalajs.dom
import org.scalajs.dom.raw.Element
import org.scalajs.dom.svg.SVG
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._
import org.scalajs.dom.ext._

class NotebookView(val elem: Element, val session: Session) extends BindableView
{
  self =>

  val connector: WebSocketTransport = WebSocketTransport("notebook", "guest" + Math.random() * 1000)

  val (input: Var[KappaMessage], output: Var[KappaMessage]) = connector.IO

  val serverErrors = Var(ServerErrors.empty)

  val kappaServerErrors = Var(KappaServerErrors.empty)

  val currentProject: Var[CurrentProject] = Var(CurrentProject.fromKappaProject(KappaProject.default))

  val sourceMap: Var[Map[String, KappaFile]] = currentProject.extractVar(p=>p.sourceMap)((p, s)=>p.copy(sourceMap = s))

  val location: Var[Bookmark] = Var(Bookmark("", 1, Nil))

  val figures: Var[Map[String, Figure]] = Var(Map.empty)

  val currentProjectName = currentProject.map(_.name)

  val editorsUpdates: Var[EditorUpdates] = Var(EditorUpdates.empty)

  val serverConfiguration: Var[ServerConnections] = Var(ServerConnections.default)

  lazy val scrollable: Element = sq.byId("MainRow").get//this.viewElement//.parentElement


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
      messages.foreach(mess=> input() = mess) //flatmapping

    case ProjectResponses.LoadedProject(proj) =>
      //println("LOADED PROJECT IS")
      //pprint.log(proj)
      currentProject() = CurrentProject.fromKappaProject(proj)


    case KappaMessage.ServerResponse(server, ers: ServerErrors) =>  serverErrors() = ers

    case KappaMessage.ServerResponse(server, ers: KappaServerErrors) => kappaServerErrors() = ers

    case Failed(operation, ers, username) =>  kappaServerErrors() = kappaServerErrors.now.copy(errors = kappaServerErrors.now.errors ++ ers)

    case other => //do nothing
  }

  val commentManager = new CommentsWatcher(editorsUpdates, location, figures, currentProjectName, input)

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
        elem.parentElement
        new MainMenuView(el, input, scrollable, menu).withBinder(n => new CodeBinder(n))
    }
    .register("ProjectsPanel"){
      case (el, args) =>
        val v = new ProjectsPanelView(el, currentProject, connector.input, connector.output).withBinder(n => new CodeBinder(n))
        addMenuItem(el, MainTabs.Projects)
        v
     }
    .register("VisualPanel"){
      case (el, args) =>
        val v = new VisualPanelView(el, kappaWatcher, input).withBinder(n => new CodeBinder(n))
        addMenuItem(el, MainTabs.Visualizations)
        v
    }
    .register("KappaEditor"){
      case (el, args) =>
        val editor = new KappaCodeEditor(el, sourceMap, input, output, kappaCursor, editorsUpdates, serverConfiguration).withBinder(n => new CodeBinder(n))
        addMenuItem(el, MainTabs.Editor)
        editor
    }
    .register("Simulations") {
      case (el, params) =>
        val v = new SimulationsView(el, sourceMap, input, output, serverConfiguration).withBinder(new CodeBinder(_))
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
        val v = new FiguresView(el, figures, input).withBinder(new CodeBinder(_))
        addMenuItem(el, MainTabs.Figures)
        v
    }
}


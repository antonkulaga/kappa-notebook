package org.denigma.kappa.notebook.views

import org.denigma.binding.extensions._
import org.denigma.binding.views.BindableView
import org.denigma.controls.code.CodeBinder
import org.denigma.kappa.messages.ServerMessages.KappaServerErrors
import org.denigma.kappa.messages._
import org.denigma.kappa.notebook._
import org.denigma.kappa.notebook.actions.Movements
import org.denigma.kappa.notebook.graph.drawing.SvgBundle.all._
import org.denigma.kappa.notebook.views.comments.{CommentsWatcher, KappaWatcher}
import org.denigma.kappa.notebook.views.common.{FixedBinder, ServerConnections}
import org.denigma.kappa.notebook.views.editor._
import org.denigma.kappa.notebook.views.figures.{Figure, FiguresView}
import org.denigma.kappa.notebook.views.menus.MainMenuView
import org.denigma.kappa.notebook.views.papers.PapersView
import org.denigma.kappa.notebook.views.project.ProjectsPanelView
import org.denigma.kappa.notebook.views.settings.{AnnotationMode, SettingsView}
import org.denigma.kappa.notebook.views.simulations.SimulationsView
import org.denigma.kappa.notebook.views.visual.VisualPanelView
import org.scalajs.dom.raw.Element
import org.scalajs.dom.svg.SVG
import org.threeten.bp._
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

class NotebookView(val elem: Element, username: String) extends BindableView
{
  self =>

  val connector: WebSocketTransport = WebSocketTransport("notebook", username)

  val (input: Var[KappaMessage], output: Var[KappaMessage]) = connector.IO

  val serverErrors = Var(ServerErrors.empty)

  val kappaServerErrors = Var(KappaServerErrors.empty)

  val currentProject: Var[KappaProject] = Var(KappaProject.default)

  val sourceMap: Var[Map[String, KappaSourceFile]] = currentProject.extractVar(p=>p.sourceMap)((p, s)=>p.copy(folder = p.folder.addFiles(sourceMap.now.values.toList)))

  val currentProjectName: Rx[String] = currentProject.map(_.name)

  val papers = currentProject.map(p=>p.papers.map(p => (p.path, p)).toMap)

  val figures: Var[Map[String, Figure]] = Var(Map.empty)

  val editorsUpdates: Var[EditorUpdates] = Var(EditorUpdates.empty)

  val serverConfiguration: Var[ServerConnections] = Var(ServerConnections.default)

  lazy val scrollable: Element = sq.byId("MainRow").get//this.viewElement//.parentElement

  lazy val lastMessageTime: Var[LocalDateTime] = Var(LocalDateTime.now) //crashes!!!

  val serverActive = Var(false)

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

  protected def goMessage(messages: List[KappaMessage], delay: Int): Unit = messages match {
    case Nil =>
    case head::tail =>
      input() = head
      scalajs.js.timers.setTimeout(delay)(goMessage(tail, delay))
  }

  protected def onMessage(message: KappaMessage): Unit = message match {

    case KappaMessage.Container(messages, 0) =>

      messages.foreach(mess=> input() = mess) //flatmapping

    case KappaMessage.Container(messages, delay) =>

      goMessage(messages, delay)

    case ProjectResponses.LoadedProject(proj) =>
      //println("LOADED PROJECT IS")
      //pprint.log(proj)
      currentProject() = proj


    case KappaMessage.ServerResponse(server, ers: ServerErrors) =>  serverErrors() = ers

    case KappaMessage.ServerResponse(server, ers: KappaServerErrors) => kappaServerErrors() = ers

    case Failed(operation, ers, username) =>  kappaServerErrors() = kappaServerErrors.now.copy(errors = kappaServerErrors.now.errors ++ ers)


    case other => //do nothing
  }

 //import org.denigma.kappa.notebook.graph.drawing.SvgBundle.all
  import scalatags.JsDom.all
  lazy val s: SVG = {
    val t = svg(all.id := "canvas"/*width :=  defaultWidth, height := defaultHeight*/).render
    t.style.position = "absolute"
    t.style.top = "-9999"
    elem.appendChild(t)
    t
  }

  val kappaCursor: Var[KappaCursor] = Var(EmptyCursor)
  //val kappaWatcher: KappaWatcher = new KappaWatcher(kappaCursor, editorsUpdates)

  val menu: Var[List[(String, Element)]] = Var(List.empty[(String, Element)])
  lazy val annotationMode = Var(AnnotationMode.ToAnnotation)
  lazy val movements = new Movements(annotationMode)

  val commentManager = new CommentsWatcher(editorsUpdates, input, movements)

  protected def addMenuItem(el: Element, title: String) = {
    menu() = menu.now :+ (title, el)
  }

  override lazy val injector = defaultInjector
    .register("MainMenuView") {
      case (el, args) =>
        elem.parentElement
        new MainMenuView(el, input, scrollable, menu, movements).withBinder(n => new CodeBinder(n))
    }
    .register("ProjectsPanel"){
      case (el, args) =>
        val v = new ProjectsPanelView(el, currentProject, connector.input, connector.output, movements).withBinder(n => new CodeBinder(n))
        addMenuItem(el, MainTabs.Projects)
        v
     }
    .register("KappaEditor"){
      case (el, args) =>
        val editor = new KappaCodeEditor(el, sourceMap, input, output, kappaCursor, editorsUpdates, serverConfiguration, movements).withBinder(n => new CodeBinder(n))
        addMenuItem(el, MainTabs.Editor)
        editor
    }
    .register("VisualPanel"){
      case (el, args) =>
        val kappaWatcher = new KappaWatcher(kappaCursor, editorsUpdates)
        val v = new VisualPanelView(el, kappaWatcher.text, kappaWatcher.parsed, input, s).withBinder(n => new FixedBinder(n))
        addMenuItem(el, MainTabs.Visualizations)
        v
    }
    .register("Simulations") {
      case (el, params) =>
        val v = new SimulationsView(el, sourceMap, input, output, serverConfiguration).withBinder(s=>new CodeBinder(s))
        addMenuItem(el, MainTabs.Simulations)
        v
    }
    .register("Figures") {
      case (el, params) =>
        val v = new FiguresView(el, figures, input, kappaCursor).withBinder(new CodeBinder(_))
        addMenuItem(el, MainTabs.Figures)
        v
    }
    .register("Papers") {
      case (el, params) =>
        val v = new PapersView(el, connector, papers, kappaCursor).withBinder(new CodeBinder(_))
        addMenuItem(el, MainTabs.Papers)
        v
    }
    .register("Settings") {
      case (el, params) =>
        val v = new SettingsView(el, connector.input, annotationMode).withBinder(new FixedBinder(_))
        addMenuItem(el, MainTabs.Settings)
        v
    }
}


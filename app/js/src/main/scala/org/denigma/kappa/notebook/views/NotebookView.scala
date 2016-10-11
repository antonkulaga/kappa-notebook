package org.denigma.kappa.notebook.views

import org.denigma.binding.extensions._
import org.denigma.binding.views.BindableView
import org.denigma.controls.code.CodeBinder
import org.denigma.kappa.messages._
import org.denigma.kappa.notebook._
import org.denigma.kappa.notebook.actions.AnimationsCircuit
import org.denigma.kappa.notebook.circuits._
import org.denigma.kappa.notebook.graph.drawing.SvgBundle.all._
import org.denigma.kappa.notebook.views.comments.KappaWatcher
import org.denigma.kappa.notebook.views.editor._
import org.denigma.kappa.notebook.views.figures.FiguresView
import org.denigma.kappa.notebook.views.menus.MainMenuView
import org.denigma.kappa.notebook.views.papers.{PapersView, WebSocketPaperLoader}
import org.denigma.kappa.notebook.views.project.ProjectsPanelView
import org.denigma.kappa.notebook.views.settings.SettingsView
import org.denigma.kappa.notebook.views.simulations.SimulationsView
import org.denigma.kappa.notebook.views.visual.VisualPanelView
import org.scalajs.dom
import org.scalajs.dom.raw.Element
import org.scalajs.dom.svg.SVG
import org.threeten.bp._
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

/**
  * The most important view of kappa-notebook, initialize almost all the logic and subviews
  * @param elem
  * @param username
  */
class NotebookView(val elem: Element, username: String) extends BindableView
{
  self =>

  val connector: WebSocketTransport = WebSocketTransport("notebook", username)

  val (fromServer: Rx[KappaMessage], toServer: Var[KappaMessage]) = connector.IO

  val interfaceIO: Var[UIMessage] = Var(EmptyUIMEssage)
  fromServer.onChange{
    case mess: UIMessage => interfaceIO() = mess //for collaboration we allow to send ui messages from the server
    case other => //do nothing
  }

  val input: Var[KappaMessage] = Var(EmptyKappaMessage) //combines to inputs

  fromServer.onChange{ message => input() = message }
  interfaceIO.onChange{ messageUI => input() = messageUI }

  val output: Var[KappaMessage] = Var(EmptyKappaMessage)
  output.onChange{
   /*
    case message: UIMessage => //TODO: check if it is safe
      interfaceIO() = message
      */
    serverMessage => toServer() = serverMessage
  }

  lazy val settings = new SettingsCircuit(input, output)

  lazy val currentServer = settings.websimConnections.now.currentServer

  lazy val animations = new AnimationsCircuit(input, output, notebookCircuit.currentProject, settings.annotationMode)

  lazy val notebookCircuit = new NotebookCircuit(input, output)

  lazy val simulationsCircuit = new SimulationsCircuit(input, output, notebookCircuit.currentProject, settings.websimConnections)

  lazy val errorsCircuit = new ErrorsCircuit(input, output, simulationsCircuit.runConfiguration)

  lazy val editorCircuit = new KappaEditorCircuit(input, output, simulationsCircuit.runConfiguration)

  lazy val currentProjectName: Rx[String] = notebookCircuit.currentProject.map(_.name)

  lazy val paperLoader = WebSocketPaperLoader(connector, notebookCircuit.currentProject.map(p=>p.paperMap))

  lazy val papersCircuit = new PaperCircuit(input, output, paperLoader)

  lazy val scrollable: Element = sq.byId("MainRow").get//this.viewElement//.parentElement

  lazy val lastMessageTime: Var[LocalDateTime] = Var(LocalDateTime.now) //crashes!!!

  val serverActive = Var(false)

  override def bindView() = {
    super.bindView()
    settings.activate()
    animations.activate()
    notebookCircuit.activate()
    simulationsCircuit.activate()
    editorCircuit.activate()
    errorsCircuit.activate()
    papersCircuit.activate()
    connector.onOpen.triggerLater{
      dom.console.log("websocket opened")
      val toLoad = ProjectRequests.Load(KappaProject.default)
      connector.output() = toLoad //ask to load default project
    }
    connector.open()
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
        val v = new ProjectsPanelView(el, notebookCircuit).withBinder(n => new CodeBinder(n))
        addMenuItem(el, MainTabs.Projects)
        v
     }
    .register("KappaEditor"){
      case (el, args) =>
        val ed = new KappaCodeEditor(el, editorCircuit, errorsCircuit).withBinder(n => new CodeBinder(n))
        addMenuItem(el, MainTabs.Editor)
        ed
    }
    .register("VisualPanel"){
      case (el, args) =>
        val kappaWatcher = new KappaWatcher(editorCircuit.kappaCursor, editorCircuit.editorsUpdates)
        val v = new VisualPanelView(el, kappaWatcher.text, kappaWatcher.parsed, input, s).withBinder(n => new CodeBinder(n))
        addMenuItem(el, MainTabs.Visualizations)
        v
    }
    .register("Simulations") {
      case (el, params) =>
        val v = new SimulationsView(el, simulationsCircuit, errorsCircuit).withBinder(s=>new CodeBinder(s))
        addMenuItem(el, MainTabs.Simulations)
        v
    }
    .register("Figures") {
      case (el, params) =>
        val v = new FiguresView(el, input, editorCircuit.kappaCursor).withBinder(new CodeBinder(_))
        addMenuItem(el, MainTabs.Figures)
        v
    }
    .register("Papers") {
      case (el, params) =>
        val v = new PapersView(el, papersCircuit, editorCircuit.kappaCursor).withBinder(new CodeBinder(_))
        addMenuItem(el, MainTabs.Papers)
        v
    }
    .register("Settings") {
      case (el, params) =>
        val v = new SettingsView(el, settings).withBinder(new CodeBinder(_))
        addMenuItem(el, MainTabs.Settings)
        v
    }
}


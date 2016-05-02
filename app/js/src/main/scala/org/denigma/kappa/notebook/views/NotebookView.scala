package org.denigma.kappa.notebook.views


import org.denigma.binding.binders.Events
import org.denigma.binding.extensions._
import org.denigma.binding.views.BindableView
import org.denigma.controls.code.CodeBinder
import org.denigma.controls.login.Session
import org.denigma.controls.sockets.WebSocketSubscriber
import org.denigma.kappa.messages._
import org.denigma.kappa.model.KappaModel
import org.denigma.kappa.notebook.views.editor.{CommentsWatcher, EditorUpdates, KappaCodeEditor, KappaWatcher}
import org.denigma.kappa.notebook.views.project.{ProjectFilesView, ProjectsView}
import org.denigma.kappa.notebook.views.visual._
import org.denigma.kappa.notebook.views.visual.drawing.SvgBundle
import org.denigma.kappa.notebook.views.visual.drawing.SvgBundle.all._
import org.denigma.kappa.notebook.{KappaHub, WebSocketTransport}
import org.scalajs.dom
import org.scalajs.dom.MouseEvent
import org.scalajs.dom.raw.Element
import org.scalajs.dom.svg.SVG
import rx.Ctx.Owner.Unsafe.Unsafe
import rx.Rx.Dynamic
import rx._

import scala.collection.immutable.SortedSet


class NotebookView(val elem: Element, val session: Session) extends BindableView
{
  self =>

  lazy val subscriber = WebSocketSubscriber("notebook", "guest" + Math.random() * 1000)

  val hub: KappaHub = KappaHub.empty

  val loaded: Var[Loaded] = Var(Loaded.empty)
  loaded.onChange{
    case ld=>
      println("sourcemap = "+ld.project.sourceMap)
      sourceMap.set(ld.project.sourceMap)
      name() = ld.project.name
      //currentProject() = ld.project
  }


  val name = Var("HelloWorld!")

  val path = Var("files")

  val sourceMap = Var(Map.empty[String, KappaFile])
  //val sources: Var[SortedSet[KappaFile]] = Var(SortedSet.empty)

  val currentProject = Rx.apply{
    val sourceFiles = sourceMap().values.toSeq
    val fls = SortedSet(sourceFiles:_*)
    val folder = KappaFolder(path(), files = fls)
    KappaProject(name(), folder)
  }

  val projectList: Rx[List[KappaProject]] = loaded.map(l=>l.other)
  val errors = Var(List.empty[String])

  subscriber.onOpen.triggerLater{
    println("websocket opened")
  }

  val connector: WebSocketTransport = WebSocketTransport(subscriber, errors){

      case ld: Loaded => loaded() = ld
      case SyntaxErrors(server, ers, params) =>
        errors() = ers
      case SimulationResult(server, status, token, params) =>
        println("percent: "+ status.percentage)
        hub.simulations() = hub.simulations.now.updated((token, params.getOrElse(status.runParameters)), status)
        if(errors.now.nonEmpty) errors() = List.empty
      case message: Connected => //nothing yet
      case ServerErrors(ers) => errors() = ers
      case other => dom.console.error(s"UNKNOWN KAPPA MESSAGE RECEIVED! "+other)
  }

  hub.runParameters.triggerLater{
    val launchParams = LaunchModel("", hub.runParameters.now)
    println("RUNNING: "+launchParams)
    connector.send(launchParams)
  }

  val editorsUpdates: Var[EditorUpdates] = Var(EditorUpdates.empty) //collect updates of all editors together

  val commentManager = new CommentsWatcher(editorsUpdates, hub)

  lazy val s: SVG = {
    val t = svg(/*width :=  defaultWidth, height := defaultHeight*/).render
    t.style.position = "absolute"
    t.style.top = "-9999"
    elem.appendChild(t)
    t
  }

  val kappaWatcher = new KappaWatcher(hub.kappaCursor, editorsUpdates, s)
  val left2right: Rx[Boolean] = kappaWatcher.direction.map{
    case KappaModel.BothDirections | KappaModel.Left2Right=> true
    case _=> false
  }

  val currentLine: Rx[String] = kappaWatcher.text

  val inRule = kappaWatcher.leftPattern

  val right2left: Rx[Boolean] = kappaWatcher.direction.map{
    case KappaModel.BothDirections | KappaModel.Right2Left=> true
    case _=> false
  }

  lazy val projectChanged = Rx{
    loaded().project == currentProject()
  }


  override lazy val injector = defaultInjector
     .register("KappaEditor"){
       case (el, args) =>
         val editor = new KappaCodeEditor(el, sourceMap, hub.selector, errors, hub.kappaCursor, editorsUpdates).withBinder(n => new CodeBinder(n))
         editor
     }
     .register("Tabs")((el, args) => new TabsView(el, hub).withBinder(n => new CodeBinder(n)))
     .register("ProjectsView")((el, args) => new ProjectsView(el, projectList).withBinder(n => new CodeBinder(n)))
     .register("ProjectFilesView")((el, args) => new ProjectFilesView(el, currentProject.map(p=>p.folder.files)).withBinder(n => new CodeBinder(n)))
     .register("LeftGraph") {  (el, args) =>
       new GraphView(el,
         kappaWatcher.leftPattern.nodes,
         kappaWatcher.leftPattern.edges,
         kappaWatcher.leftPattern.layouts,
         args.getOrElse("container","graph-container").toString).withBinder(n => new CodeBinder(n)) }
     .register("RightGraph") {  (el, args) =>
       new GraphView(el,
         kappaWatcher.rightPattern.nodes,
         kappaWatcher.rightPattern.edges,
         kappaWatcher.rightPattern.layouts,
         args.getOrElse("container","graph-container").toString).withBinder(n => new CodeBinder(n)) }
     //.register("Files") {  (el, args) => new FilesView(el, hub.path).withBinder(n => new CodeBinder(n)) }

}

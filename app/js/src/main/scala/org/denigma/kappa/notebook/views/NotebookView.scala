package org.denigma.kappa.notebook.views


import org.denigma.binding.extensions._
import org.denigma.binding.views.BindableView
import org.denigma.codemirror.{Editor, PositionLike}
import org.denigma.controls.code.CodeBinder
import org.denigma.controls.login.Session
import org.denigma.controls.papers.Bookmark
import org.denigma.kappa.messages._
import org.denigma.kappa.model.KappaModel
import org.denigma.kappa.notebook.views.editor.{CommentsWatcher, EditorUpdates, KappaCodeEditor, KappaWatcher}
import org.denigma.kappa.notebook.views.project.{ProjectFilesView, ProjectsView}
import org.denigma.kappa.notebook.views.visual._
import org.denigma.kappa.notebook.views.visual.drawing.SvgBundle.all._
import org.denigma.kappa.notebook.{Selector, WebSocketTransport}
import org.scalajs.dom.raw.Element
import org.scalajs.dom.svg.SVG
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

import scala.collection.immutable.SortedSet


class NotebookView(val elem: Element, val session: Session) extends BindableView
{
  self =>

  val connector: WebSocketTransport = WebSocketTransport("notebook", "guest" + Math.random() * 1000)

  val input = connector.onKappaMessage

  val output = connector.sendKappaMessage

  val onopen = connector.onOpen

  val papers: Var[Map[String, Bookmark]] = Var(Map.empty)

  val selector = Selector.default

  val loaded: Var[FileResponses.Loaded] = Var(FileResponses.Loaded.empty)

  val projectList: Rx[List[KappaProject]] = loaded.map(l=>l.projects)

  val errors = Var(List.empty[String])

  val name = Var("HelloWorld!")

  val path = Var("files")

  val sourceMap = Var(Map.empty[String, KappaFile])
  //val sources: Var[SortedSet[KappaFile]] = Var(SortedSet.empty)

  val currentProject: Rx[KappaProject] = Rx{
    val sourceFiles = sourceMap().values.toSeq
    val fls = SortedSet(sourceFiles:_*)
    val folder = KappaFolder(path(), files = fls)
    println("FILES ARE: "+fls)
    KappaProject(name(), folder)
  }


  override def bindView() = {
    super.bindView()
    loaded.onChange{
      case ld=>
        sourceMap.set(ld.project.sourceMap)
        name() = ld.project.name
      //currentProject() = ld.project
    }
    connector.onOpen.triggerLater{
      println("websocket opened")
      output() = FileRequests.Load(KappaProject.default) //ask to load default project
    }
    connector.onKappaMessage.foreach{
      case ld: FileResponses.Loaded => loaded() = ld
      case SyntaxErrors(server, ers, params) =>  errors() = ers
      case ServerErrors(ers) => errors() = ers
      case other => //do nothing
    }
    connector.open()
  }

  /*
  protected def onMessage(message: KappaMessage): Unit = message match {
    case EmptyKappaMessage => //do nothing
    case ld: Loaded => loaded() = ld
    case SyntaxErrors(server, ers, params) =>  errors() = ers
    case SimulationResult(server, status, token, params) =>
      println("percent: "+ status.percentage)
      simulations() = simulations.now.updated((token, params.getOrElse(status.runParameters)), status)
      if(errors.now.nonEmpty) errors() = List.empty
    case message: Connected => //nothing yet
    case ServerErrors(ers) => errors() = ers
    case other => dom.console.error(s"UNKNOWN KAPPA MESSAGE RECEIVED! "+other)
  }
  */



  protected def concat() = {
    sourceMap.now.values.foldLeft(""){
      case (acc, e)=> acc + e.content
    }
  }


  val editorsUpdates: Var[EditorUpdates] = Var(EditorUpdates.empty) //collect updates of all editors together

  val commentManager = new CommentsWatcher(editorsUpdates, papers, selector)

  lazy val s: SVG = {
    val t = svg(/*width :=  defaultWidth, height := defaultHeight*/).render
    t.style.position = "absolute"
    t.style.top = "-9999"
    elem.appendChild(t)
    t
  }

  val kappaCursor: Var[Option[(Editor, PositionLike)]] = Var(None)

  val kappaWatcher = new KappaWatcher(kappaCursor, editorsUpdates, s)


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
    loaded().projectOpt == Some(currentProject())
  }


  override lazy val injector = defaultInjector
     .register("KappaEditor"){
       case (el, args) =>
         val editor = new KappaCodeEditor(el, sourceMap, selector, errors, kappaCursor, editorsUpdates).withBinder(n => new CodeBinder(n))
         editor
     }
     .register("Tabs")((el, args) => new TabsView(el, input, output, selector, papers, kappaCursor).withBinder(n => new CodeBinder(n)))

     //.register("ProjectsPanel")((el, args) => new ProjectsPanelView(el, currentProject, projectList).withBinder(n => new CodeBinder(n)))

     .register("ProjectsView")((el, args) => new ProjectsView(el, /*currentProject,*/ projectList).withBinder(n => new CodeBinder(n)))
     .register("ProjectFilesView")((el, args) => new ProjectFilesView(el, currentProject).withBinder(n => new CodeBinder(n)))
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

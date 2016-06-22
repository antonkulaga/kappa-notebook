package org.denigma.kappa.notebook.views

import org.denigma.binding.binders._
import org.denigma.binding.views.{BindableView, ItemsSeqView}
import org.denigma.controls.code.CodeBinder
import org.scalajs.dom
import org.scalajs.dom._
import org.scalajs.dom.raw.{HTMLElement, Element, SVGElement}
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe
import org.denigma.binding.extensions._
import org.scalajs.dom.ext._

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport

class MainMenuView(val elem: Element, val items: Var[List[(String, Element)]] ) extends BindableView with ItemsSeqView{

  type Item =  (String, Element)
  type ItemView = MainMenuItemView
/*

  val onDragOver = Var(Events.createDragEvent())
  onDragOver.onChange{
    case event=>
      event.preventDefault()
      event.dataTransfer.dropEffect = "copy"
      println("drag over parent")
    //event.preventDefault()
      //println("DRAG Over WITH:"+elem.id)
  }
*/


  override protected def onMove(mv: Moved[Item]) = {
    println(" MOVED = "+mv)
    val fr = itemViews.now(items.now(mv.from))
    val t = itemViews.now(items.now(mv.to))
    this.replace(t.viewElement, fr.viewElement)
  }


  override protected def subscribeUpdates() = {
    template.hide()
    this.items.now.foreach(i => this.addItemView(i, this.newItemView(i)))
    updates.onChange(upd => {
      upd.added.foreach(onInsert)
      upd.added.foreach(i=>println("inserted = "+i))
      upd.removed.foreach(r=>println("removed = "+r))
      upd.moved.foreach(r=>println("moved = "+r))
      upd.removed.foreach(onRemove)
      upd.moved.foreach(onMove)
    })
  }

  protected def insertNear(from: Item, to: Item) = {
    val its = items.now
    items() = its.foldLeft(List.empty[Item]){
      case (acc, el) if el ==from => to::acc
      case (acc, el) if el == to => from::acc
      case (acc, el) => el::acc
    }.reverse

    /*
    (its.indexOf(from), its.indexOf(to)) match {
      case (-1, _) => dom.console.error("cannot find first view")
      case (_, -1) => dom.console.error("cannot find second view")


      /*
    case (f, t) if f < t=>
      println("old items = "+its.map(_._1))
      items() = (its.take(t).filterNot(_==from) :+ from :+ to) ++ its.drop(t+1)
      println("new items = "+items.now.map(_._1))
    case (f, t) if f>t =>
      println("old items = "+its.map(_._1))
      items() = (its.take(t) :+ to :+ from) ++ its.drop(t+1).filterNot(_==from)
      println("new items = "+items.now.map(_._1))
      */
      case other => println("OTHER ="+other)
    }
    */
  }

  def park(to: ItemView, targetName: String) = {
    items.now.find{ case (key, _) => key == targetName } match {
      case Some(from) =>
        val e = itemViews.now(from).elem
        if( !( elem.children.contains(e) && e.parentElement == elem ) )
        {
          dom.console.error("does not contain drop target!")
          println(e.outerHTML)
          elem.appendChild(e)
        }
        println("switch = "+from+" => "+to.item)
        insertNear(from, to.item)

      case None =>
        dom.console.error("cannot find the draggable element")
    }
  }

  /*
  def park(source: ItemView, elm: Element): Unit = elm match {
    case e if this.subviews.contains(e.id)  =>
      val otherChild: ChildView = subviews(e.id)
      itemViews.now.values.collectFirst{ case value if value == otherChild => value } match {
        case None => println("cannot find child")
        case Some(v) => insertNear(source, v)
      }

    case other =>
      println("other = "+other.outerHTML)
      other.parentElement match {
        case null =>
        case p if p!= elem =>
        case e => park(source, e)
      }
  }
  */


  override def newItemView(item: (String, Element)): MainMenuItemView = this.constructItemView(item){
    case (el: HTMLElement, _) => new MainMenuItemView(el, item).withBinder(new CodeBinder(_))
    case _ => throw new Exception("Element is not an HTML Element")
  }
}

@JSExport
class MainMenuItemView(val elem: HTMLElement, val item: (String, Element)) extends BindableView {

  self =>

  val itemName: Var[String] = Var(item._1)
  val target: Element = item._2

  lazy val menuParent = this.fromParents{
    case p: MainMenuView => p
  }

  val onDragOver = Var(Events.createDragEvent())
  onDragOver.onChange{
    case event=>
      event.preventDefault()
      event.stopPropagation()
      println("drag over "+ item._1)
  }

  val onDragEnter = Var(Events.createDragEvent())
  onDragEnter.onChange{
    case event=>
      event.preventDefault()
      event.stopPropagation()
      elem.classList.add("dockable")
      event.dataTransfer.effectAllowed = "none"
      println("drag enter "+ item._1)
  }

  val onDragLeave = Var(Events.createDragEvent())
  onDragLeave.onChange{
    case event=>
      elem.classList.remove("dockable")
      event.preventDefault()
      event.stopPropagation()
    //println("DRAG LEFT WITH:"+elem.id)
  }


  val onDrop = Var(Events.createDragEvent())
  onDrop.onChange{
    case event=>
      event.preventDefault()
      event.stopPropagation()
      val nm = event.dataTransfer.getData("text/plain")
      elem.classList.remove("dockable")
      menuParent match {
        case Some(p) =>
          println("let us park!")
          p.park(self, nm)
        case _ => println("cannot park")
    }
  }

  val onDragStart = Var(Events.createDragEvent())
  onDragStart.onChange{
    case event=>
      event.dataTransfer.setData("text/plain", item._1)
      //event.dataTransfer.effectAllowed = "link"
  }

  val onDragEnd = Var(Events.createDragEvent())
  onDragEnd.onChange{
    case event=>
      println("DRAG ended for:"+itemName.now)
  }

  val menuClick = Var(Events.createMouseEvent())

  @JSExport
  def click(): Unit = {
    dom.window.history.pushState(scalajs.js.Dynamic.literal(id = target.id), itemName.now, target.id)
  }

  menuClick.triggerLater{ click() }
}
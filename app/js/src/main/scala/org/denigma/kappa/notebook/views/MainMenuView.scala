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

class MainMenuView(val elem: Element, val items: Var[List[(String, Element)]] ) extends BindableView with ItemsSeqView{

  type Item =  (String, Element)
  type ItemView = MainMenuItemView

  val onDragOver= Var(Events.createDragEvent())
  onDragOver.onChange{
    case event=>
      event.preventDefault()
      println("DRAG Over WITH:"+elem.id)
  }

  val onDragEnter = Var(Events.createDragEvent())
  onDragEnter.onChange{
    case event=>
      event.preventDefault()
      elem.classList.add("dockable")
      println("DRAG ENTER WITH:"+elem.id)
      println("OUTTER:"+elem.outerHTML)
  }

  val onDragLeave = Var(Events.createDragEvent())
  onDragLeave.onChange{
    case event=>
      elem.classList.remove("dockable")
      println("DRAG LEFT WITH:"+elem.id)
  }

  protected def insertNear(from: ItemView, to: ItemView) = {
    println("CHILDREN SWITCHING!!!!")
    (items.now.indexOf(from.item), items.now.indexOf(to.item)) match {
      case (-1, _) => dom.console.error("cannot find first view")
      case (_, -1) => dom.console.error("cannot find second view")
      case (f, t) if f<t=>
        items() = (items.now.take(t).filterNot(_==from.item) :+ from.item :+ to.item) ++ items.now.drop(t+2)
      case (f, t) if f>t =>
        items() = (items.now.take(t) :+ to.item :+ from.item) ++ items.now.drop(t+2).filterNot(_==from.item)
      case other => println("OTHER ="+other)
    }
  }


  def park(source: ItemView, elem: Element): Unit = elem match {
    case e if this.subviews.contains(e.id)  =>
      val otherChild: ChildView = subviews(e.id)
      itemViews.now.values.collectFirst{ case value if value==otherChild=> value } match {
        case None => println("cannot find child")
        case Some(v) => insertNear(source, v)
      }

    case other => other.parentElement match {
      case null =>
      case p if p!= elem =>
      case e => park(source, e)
    }
  }


  override def newItemView(item: (String, Element)): MainMenuItemView = this.constructItemView(item){
    case (el: HTMLElement, _) => new MainMenuItemView(el, item).withBinder(new CodeBinder(_))
    case _ => throw new Exception("Element is not an HTML Element")
  }
}

class MainMenuItemView(val elem: HTMLElement, val item: (String, Element)) extends BindableView {


  val itemName: Var[String] = Var(item._1)
  val target: Element = item._2

  lazy val menuParent = this.fromParents{
    case p: MainMenuView => p
  }

  val onDrop = Var(Events.createDragEvent())
  onDrop.onChange{
    case event=>
      (event.target, menuParent) match {
        case (t: Element, Some(p)) =>
          if(t==elem) {
            println("warning!!!")
            println("target = "+t.outerHTML)
            println("currenttarget = "+event.currentTarget)
          }
          p.park(this, t)
        case _ => println("cannot park")
    }
  }

  val onDragStart = Var(Events.createDragEvent())
  onDragStart.onChange{
    case ev=>
      ev.dataTransfer.dropEffect = "move"
      println("DRAG started for:"+elem.id)
  }

  val onDragEnd = Var(Events.createDragEvent())
  onDragEnd.onChange{
    case event=>
      println("DRAG ended for:"+elem.id)
  }

  val menuClick = Var(Events.createMouseEvent())
  menuClick.triggerLater{
    dom.window.history.pushState(scalajs.js.Dynamic.literal(id = target.id), itemName.now, target.id)
  }
}
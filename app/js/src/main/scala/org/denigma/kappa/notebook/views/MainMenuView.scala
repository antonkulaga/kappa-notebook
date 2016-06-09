package org.denigma.kappa.notebook.views

import org.denigma.binding.binders._
import org.denigma.binding.views.{BindableView, ItemsSeqView}
import org.scalajs.dom._
import org.scalajs.dom.raw.{HTMLElement, Element, SVGElement}
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe
import org.denigma.binding.extensions._

class MainMenuView(val elem: Element, val items: Var[List[(String, Element)]] ) extends BindableView with ItemsSeqView{

  type Item =  (String, Element)
  type ItemView = MainMenuItemView

  val onDrop = Var(Events.createDragEvent())
  onDrop.onChange{
    case event=>
      elem.classList.remove("dockable")
      println("DROPPED ITEM:"+elem.id)
  }

  val onDragOver= Var(Events.createDragEvent())
  onDragOver.onChange{
    case event=>
      event.preventDefault()
      println("DRAG Over WITH:"+elem.id)
  }

  val onDragEnter = Var(Events.createDragEvent())
  onDragEnter.onChange{
    case event=>
      elem.classList.add("dockable")
      println("DRAG ENTER WITH:"+elem.id)
  }

  val onDragLeave = Var(Events.createDragEvent())
  onDragLeave.onChange{
    case event=>
      elem.classList.remove("dockable")
      println("DRAG LEFT WITH:"+elem.id)
  }


  override def newItemView(item: (String, Element)): MainMenuItemView = this.constructItemView(item){
    case (el: HTMLElement, _) => new MainMenuItemView(el, Var(item._1), Var(item._2)).withBinder(new FixedBinder(_))
    case _ => throw new Exception("Element is not an HTML Element")
  }
}

class MainMenuItemView(val elem: HTMLElement, val itemName: Var[String], val target: Var[Element]) extends BindableView {

  val fragment = target.map(t=>"#"+t.id)
  val onDragStart = Var(Events.createDragEvent())
  onDragStart.onChange{
    case event=>
      event.dataTransfer.dropEffect = "move"
      event.dataTransfer.setData("", "")

      println("DRAG started for:"+elem.id)
  }

  val onDragEnd = Var(Events.createDragEvent())
  onDragEnd.onChange{
    case event=>
      println("DRAG ended for:"+elem.id)
  }
}
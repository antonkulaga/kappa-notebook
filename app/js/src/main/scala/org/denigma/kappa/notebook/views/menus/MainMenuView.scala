package org.denigma.kappa.notebook.views.menus

import org.denigma.binding.binders._
import org.denigma.binding.extensions._
import org.denigma.binding.views.{BindableView, ItemsSeqView}
import org.denigma.controls.code.CodeBinder
import org.denigma.kappa.messages.KappaMessage
import org.denigma.kappa.notebook.views.actions.Movements
import org.scalajs.dom
import org.scalajs.dom.raw.{Element, HTMLElement}
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

import scala.scalajs.js
import scala.scalajs.js.annotation.{ScalaJSDefined, JSExport}
import scala.util.{Failure, Success, Try}

class MainMenuView(
                   val elem: Element,
                   val input: Var[KappaMessage],
                   val scrollPanel: Element,
                   val items: Var[List[(String, Element)]] ) extends BindableView with ItemsSeqView{

  type Item =  (String, Element)
  type ItemView = MainMenuItemView

  val menuMap: Rx[Map[String, ViewElement]] = items.map(list=>list.toMap)

  lazy val zipped = items.zipped

  def insertItemView(item: Item, iv: ItemView, before: Element): ItemView = {
    Try ( template.parentElement.insertBefore(iv.viewElement, before) ) match {
      case Failure(th) =>
        dom.console.error("stack trace "+th.getMessage+"\n"+th.getStackTrace.toList.mkString("/n"))
        println("TEMPLATE ="+template.outerHTML)
        println("IV ="+iv.viewElement.outerHTML)
        println("TEMPLATE PARENT ="+template.parentElement.outerHTML)
        println("and EL = \n**********\n"+this.viewElement.outerHTML)
      case Success(res) =>
    }
    iv match {
      case b: ChildView =>  this.addView(b)
    }
    itemViews() = itemViews.now + (item->iv)
    iv.bindView()
    iv
  }

  @inline protected def reDraw(curRev: List[Item], added: Set[Item], insertBefore: Element): Unit =  curRev match {
    case Nil =>
    case head :: tail if added.contains(head) =>
      val v = this.newItemView(head)
      insertItemView(head, v, insertBefore)
      reDraw(tail, added - head, v.viewElement)
    case head :: tail =>
      val view = itemViews.now(head)
      template.parentElement.insertBefore(view.viewElement, insertBefore)
      reDraw(tail, added, view.viewElement)
  }

  override protected def subscribeUpdates() = {
    template.hide()
    this.items.now.foreach(i => this.addItemView(i, this.newItemView(i)))
    zipped.onChange{
      case (from, to) if from == to => //do nothing
      case (prev, cur) if prev !=cur =>
        val removed = prev.diff(cur)
        for(r <- removed) removeItemView(r)
        val added = cur.toSet.diff(prev.toSet)
        val revCur = cur.toList.reverse
        reDraw(revCur, added, template)
    }
  }

  protected def insertNear(from: Item, to: Item) = {
    val its = items.now
    items() = its.foldLeft(List.empty[Item]){
      case (acc, el) if el ==from => to::acc
      case (acc, el) if el == to => from::acc
      case (acc, el) => el::acc
    }.reverse
  }

  protected def switchElements(from: Element, to: Element) = {
    val nextFrom = from.nextElementSibling
    val nextTo = to.nextElementSibling
    from.parentElement.insertBefore(to, nextFrom)
    to.parentElement.insertBefore(from, nextTo)
  }

  def park(to: ItemView, targetName: String) = {
    items.now.find{ case (key, _) => key == targetName } match {
      case Some(from) =>
        insertNear(from, to.item)
        switchElements(from = from._2, to.item._2)

      case None =>
        dom.console.error("cannot find the draggable element")
    }
  }

  override def newItemView(item: (String, Element)): MainMenuItemView = this.constructItemView(item){
    case (el: HTMLElement, _) => new MainMenuItemView(el, item, input).withBinder(new CodeBinder(_))
    case _ => throw new Exception("Element is not an HTML Element")
  }

  override lazy val injector = defaultInjector
    .register("ScrollerView") {
      case (el, args) =>
        new ScrollerView(el, scrollPanel, input, menuMap).withBinder(n => new CodeBinder(n))
    }
}

@JSExport
class MainMenuItemView(
                        val elem: HTMLElement,
                        val item: (String, Element),
                        val input: Var[KappaMessage]
                      ) extends BindableView {

  self =>

  val itemName: Var[String] = Var(item._1)
  val target: Element = item._2

  lazy val menuParent = this.fromParents{
    case p: MainMenuView => p
  }

  val visible: Var[Boolean] = Var(true)
  visible.onChange{
    case true => if(target.display == "none") target.display = "block"
    case false => target.display = "none"
  }

  val onDragOver = Var(Events.createDragEvent())
  onDragOver.onChange{
    case event=>
      event.preventDefault()
      event.stopPropagation()
      //println("drag over "+ item._1)
  }

  val onDragEnter = Var(Events.createDragEvent())
  onDragEnter.onChange{
    case event=>
      event.preventDefault()
      event.stopPropagation()
      elem.classList.add("dockable")
      event.dataTransfer.effectAllowed = "none"
      //println("drag enter "+ item._1)
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
          //println("let us park!")
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
    input() = Movements.toTab(itemName.now)
  }

  menuClick.triggerLater{ click() }
}

@ScalaJSDefined
class ScrollPosition(val id: String, val currentPosition: Double) extends js.Object {

}
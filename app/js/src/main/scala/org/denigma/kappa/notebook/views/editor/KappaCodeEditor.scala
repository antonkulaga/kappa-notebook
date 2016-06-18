package org.denigma.kappa.notebook.views.editor

import org.denigma.binding.binders.GeneralBinder
import org.denigma.binding.extensions._
import org.denigma.binding.views.{BindableView, ItemsMapView}
import org.denigma.codemirror._
import org.denigma.kappa.messages.{SyntaxErrors, Go, KappaFile, KappaMessage}
import org.denigma.kappa.notebook.views.common.TabHeaders
import org.scalajs.dom.raw.Element
import rx.Ctx.Owner.Unsafe.Unsafe
import rx.Rx.Dynamic
import rx._

import scala.collection.immutable._


class KappaCodeEditor(val elem: Element,
                      val items: Var[Map[String, KappaFile]],
                      val input: Var[KappaMessage],
                      val kappaCursor: Var[Option[(Editor, PositionLike)]],
                      val editorUpdates: Var[EditorUpdates]) extends BindableView
  with ItemsMapView
{

  val selected: Var[String] = Var("")
  //items.foreach(i=>println(" FILES: "+i))

  override type Item = String

  val syntaxErrors = Var(SyntaxErrors.empty)

  val errors: Rx[String] = syntaxErrors.map(er => if(er.isEmpty) "" else er.errors.map(s=>s.message).reduce(_ + "\n" + _))

  val hasErrors = errors.map(e=>e != "")

  val headers = itemViews.map(its=> SortedSet.empty[String] ++ its.values.map(_.id))

  override lazy val injector = defaultInjector
    .register("headers")((el, args) => new TabHeaders(el, headers, selected).withBinder(new GeneralBinder(_)))

  override type Value = KappaFile

  override type ItemView = CodeTab


  input.onChange{
    case Go.ToSource(name, from, to)=>
      selected() = name

    case s: SyntaxErrors=>
      syntaxErrors() = s

    case other => //do nothing
  }



  protected def keyVar(key: Key) = {
    require(items.now.contains(key), s"we are adding an Item view for key(${key}) that does not exist")
    val initialValue = this.items.now(key)
    val v = Var(initialValue)
    v.onChange{
      case value=>
        items() = items.now.updated(key, value)
    }
    v
    //note: killing should be done on unbinding
  }

  override def newItemView(item: Item): ItemView = this.constructItemView(item) {
    case (el, _) =>
      el.id = item //dirty trick
      val value: Var[KappaFile] = keyVar(item)
      println(s"source for  $item loaded")
      //println(value.now)
      val view: ItemView = new CodeTab(el, item, value, selected, editorUpdates, kappaCursor)//.withBinder(v => new CodeBinder(v) )
      println(selected.now)
      selected() = item
      view

  }
}

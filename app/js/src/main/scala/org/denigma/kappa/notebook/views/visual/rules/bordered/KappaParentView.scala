package org.denigma.kappa.notebook.views.visual.rules.bordered

import org.denigma.kappa.notebook.views.visual.rules.KappaView
import org.denigma.kappa.notebook.views.visual.rules.drawing.Rectangle
import org.denigma.threejs.{Object3D, Vector3}

@deprecated
trait KappaParentView extends KappaView {
  type ChildView <: KappaView

  def children: List[ChildView]

  lazy val childrenShift = new Vector3(0, 0, 1)


  def setShift(pos: Vector3): Vector3 = {
    pos.set(pos.x + childrenShift.x, pos.y + childrenShift.y, pos.z + childrenShift.z)
  }


  protected def childrenLine(): (List[(ChildView, Rectangle)], Double) = {
    children.foldLeft(List.empty[(ChildView, Rectangle)], 0.0) {
      case ((list, cw), child) =>
        val box = child.textBox
        val nw = cw + box.width + child.padding * 3
        ((child, box) :: list, nw)
    }
  }

  protected def alignHor(elements: List[(ChildView, Rectangle)]): List[(ChildView, Rectangle)] =
    elements match
    {

      case Nil => Nil

      case list =>
        val rev= list.foldLeft(List.empty[(ChildView, Rectangle)]) {
          case (Nil, (child, box)) =>
            (child, box.copy(x = 0))::Nil

          case ((prev, prevBox) :: tail, (child, box)) =>
            val nx = prevBox.right + child.padding * 2
            (child, box.copy(x = nx))::(prev, prevBox)::tail
        }
        val totalWidth = rev.head._2.right
        val start = -(totalWidth / 2)//(Math.max(this.textBox.width - totalWidth, 0) - textBox.width) / 2
        //println(s"start(${start}) width${textBox.width} totalWidth${totalWidth}")
        //reversing back and shifting according to start value
        rev.foldLeft(List.empty[(ChildView, Rectangle)]) {
          case ( acc, (sd, box)) =>
            val nx = box.x + start + box.width / 2
            //println("BOX = "+ box.copy(x = nx)+" start = " + start)
            (sd, box.copy(x = nx))::acc
        }// -> Math.max(rev.head._2.right, parentWidth)
    }

  protected def alignVert(list: List[(ChildView, Rectangle)]): List[(ChildView, Rectangle)] = if(list.isEmpty) Nil else {
    val rev: List[(ChildView, Rectangle)] = list.foldLeft(List.empty[(ChildView, Rectangle)]) {
      case (Nil, (sd, box)) =>
        (sd, box.copy(y = 0))::Nil

      case ( (prev, prevBox) :: tail, (child, box)) =>
        val ny = prevBox.bottom + padding * 2
        (child, box.copy(y = ny))::(prev, prevBox)::tail
    }
    val totalHeight = rev.head._2.bottom
    //val start = Math.max((textBox.height - totalHeight) / 2, 0)
    val start = (Math.max(this.textBox.height - totalHeight, 0) - textBox.height) / 2
    rev.foldLeft(List.empty[(ChildView, Rectangle)]) {
      case ( acc, (sd, box)) =>
        val ny = box.y + start + box.height / 2
        (sd, box.copy(y = ny))::acc
    }// -> Math.max(rev.head._2.right, parentWidth)
  }

  def drawChildren(updateChildren: Boolean): Unit = {

    //val (boxes: List[(T, Rectangle)], w: Double) = inlineSides(sides, sideFontSize, sidePadding * 3)
    val (boxes: List[(ChildView, Rectangle)], w: Double) = childrenLine()
    val border = SideBorder.extract(textBox.width, boxes.reverse, w, padding)
    //val (top, bottom) = (alignHor(bottom.t), alignHor())
    val left = alignVert(border.left)
    val top = alignHor(border.top)
    val bottom = alignHor(border.bottom)
    val right = alignVert(border.right)

    drawVertSides(left, -labelBox.width / 2, updateChildren, true)
    drawHorSides(top, labelBox.height - padding, updateChildren)
    drawHorSides(bottom, -labelBox.height + padding, updateChildren)
    drawVertSides(right, labelBox.width / 2, updateChildren, false)
  }

  def drawHorSides(tuples: List[(ChildView, Rectangle)], y: Double, updateChildren: Boolean): Unit = {
    for ((child, box) <- tuples) {
      val obj = child.render()
      setShift(obj.position.set(box.x, y, 0.0))
      container.add(obj)
    }
  }

  def drawVertSides(tuples: List[(ChildView, Rectangle)], x: Double, updateChildren: Boolean, left: Boolean): Unit = {
    for ((child, box) <- tuples) {
      val obj = child.render()
      val shift = if(left) - box.width / 2 else box.width / 2
      setShift(obj.position.set(x + shift, box.y, 0.0))
      container.add(obj)
    }
  }

  override def render(): Object3D = {
    super.render()
    drawChildren(true)
    container
    //container.children.foreach(c=>scene.remove(c))
  }

  override def clearChildren() = {
    container.children.toList.foreach(container.remove)
    children.foreach(_.clearChildren())
  }
}

package org.denigma.kappa.notebook.views.visual.rules.bordered

/*
class KappaSideView(val data: Side, val fontSize: Double, val padding: Double, val s: SVG) extends  KappaView
{

  type Data = Side

  type ChildView = KappaStateView


  override def gradientName: String =  "GradSide"

  protected lazy val defaultGradient =
    linearGradient(x1 := 0, x2 := 0, y1 := 0, y2 := "1", scalatags.JsDom.all.id := gradientName,
      stop(offset := "0%", stopColor := "white"),
      stop(offset := "100%", stopColor := "deepskyblue")
    )

  lazy val gradient = Var(defaultGradient)

  lazy val children: List[ChildView] = data.states.toList.map(state=> new KappaStateView(state, fontSize / 1.6, padding / 1.6, s))

}
*/
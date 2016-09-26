package org.denigma.controls.charts


trait PlotSeries extends Series{
  lazy val maxOpt = if(points.isEmpty) None else Some(Point(points.maxBy(p=>p.x).x, points.maxBy(p=>p.y).y))
  lazy val minOpt = if(points.isEmpty) None else Some(Point(points.minBy(p=>p.x).x, points.minBy(p=>p.y).y))
}

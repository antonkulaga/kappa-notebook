package org.denigma.kappa.parsers
import boopickle.DefaultBasic._
/**
  * Created by antonkulaga on 8/28/16.
  */
object AST {
  trait ASTElement

  object IRI {
    implicit lazy val pickler = boopickle.Default.generatePickler[IRI]
  }

  case class IRI(value: String) extends ASTElement
  {

    lazy val segments =  local.split("[/\\\\]").toList
    lazy val name = segments.last
    protected lazy val semicol = value.indexOf(":")

    lazy val namespace = value.substring(0, Math.max(semicol, 0))
    lazy val local = value.substring(Math.min(semicol+1, value.length))
    lazy val path = if(namespace.nonEmpty) value else local

  }

}

package org.denigma.kappa

import scala.collection.immutable.Map
import scala.reflect.macros.whitebox

/**
  * Created by antonkulaga on 11/6/15.
  */
class Extractor {
  def extract[T: c.WeakTypeTag, TE: c.WeakTypeTag](c: whitebox.Context): c.Expr[Map[String, TE]] = {
    import c.universe._

    val mapApply = Select(reify(Map).tree, TermName("apply"))

    val we = weakTypeOf[TE]

    val pairs = weakTypeOf[T].members.collect {
      case m: MethodSymbol if (m.isVal || m.isCaseAccessor || m.isGetter) && m.returnType.<:<(we) =>
        val name = c.literal(m.name.decodedName.toString)
        val value = c.Expr[T](Select(Ident(TermName("t")), m.name))
        reify(name.splice -> value.splice).tree
    }

    c.Expr[Map[String, TE]](Apply(mapApply, pairs.toList))
  }
}

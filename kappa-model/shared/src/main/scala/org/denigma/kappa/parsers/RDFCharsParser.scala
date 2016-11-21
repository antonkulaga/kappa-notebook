package org.denigma.kappa.parsers
import fastparse.all._
import fastparse.core.Parser
import fastparse.parsers.Intrinsics

trait Chars extends BasicParser{
  //def CharIn(strings: Seq[Char]*) = Intrinsics.ElemIn[Char, String]("CharIn", strings.map(_.toIndexedSeq): _*)

  lazy val ALPHA = P(CharIn('A' to 'Z') |  CharIn('a' to 'z'))

  lazy val DIGIT = P(CharIn('0' to '9')) //not in spec

  lazy val ALPHANUMERIC = P(ALPHA | DIGIT)
}

trait SimpleTypes extends Chars {
  //[20]	INTEGER	::=	[+-]? [0-9]+
  lazy val INTEGER = P(CharIn("+-").? ~ DIGIT.rep(1))

  //[21]	DECIMAL	::=	[+-]? ([0-9]* '.' [0-9]+)
  lazy val DECIMAL = P(CharIn("+-").? ~ DIGIT.rep(0) ~ "." ~DIGIT.rep(1))


  lazy val HEX = P(CharIn('0' to '9') | CharIn('A' to 'F') | CharIn('a' to 'f'))


  lazy val PERCENT = P("%" ~HEX ~ HEX)
}

trait RDFCharsParser extends SimpleTypes {

  lazy val IRIREF_CHAR = P(!CharIn("<>\"{}|^`\\") ~CharIn('\u0021' to '\uFFFE'))

  lazy val unicodeEscape = P( "\\u" ~ HEX ~ HEX ~ HEX ~ HEX )

  lazy val unicodeEscapeLarge = P( "\\U" ~ HEX ~ HEX ~ HEX ~ HEX ~ HEX ~ HEX ~ HEX ~ HEX)

  lazy val UCHAR = P( unicodeEscape | unicodeEscapeLarge) //NOTE - check

  lazy val ECHAR = P("\\" ~ CharIn("tbnrf\"\'\\\\"))

  lazy val LOCAL_ESC = CharIn("_,~.-!$&'()*+,;=/?#@%")

  lazy val PN_LOCAL_ESC = P("\\" ~ LOCAL_ESC)

  lazy val PN_CHARS_BASE: P[Unit] = P(
    CharIn('A' to 'Z') |  CharIn('a' to 'z') |
      CharIn('\u00C0' to '\u00D6') |  CharIn('\u00D8' to '\u00F6') |
      CharIn('\u00F8' to '\u02FF') |  CharIn('\u0370' to '\u037D') |
      CharIn('\u037F' to '\u1FFF') |  CharIn('\u200C' to '\u200D') |
      CharIn('\u2070' to '\u218F') |  CharIn('\u2C00' to '\u2FEF') |
      CharIn('\u3001' to '\uD7FF') |  CharIn('\uF900' to '\uFDCF') |
      CharIn('\uFDF0' to '\uFFFD')
  )

  lazy val PN_CHARS_U = P(PN_CHARS_BASE | "_" )

  lazy val PN_CHARS = P(CharIn("-/\\\\") | DIGIT | PN_CHARS_U | "\u00B7" | CharIn('\u0300'  to '\u036F') | CharIn ('\u203F' to '\u2040'))

  lazy val isHighSurrogate = P(CharPred(_.isHighSurrogate))

  lazy val isLowSurrogate = P(CharPred(_.isLowSurrogate))

  lazy val PN_CHARS_COLON = P(PN_CHARS | ":")

  lazy val PN_CHARS_U_DIGIT = P(PN_CHARS_U | DIGIT)

  lazy val PN_CHARS_U_COLON_DIGIT = P(PN_CHARS_U_DIGIT | ":")


}

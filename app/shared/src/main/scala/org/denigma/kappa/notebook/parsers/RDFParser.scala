package org.denigma.kappa.notebook.parsers

import fastparse.all
import fastparse.all._
//https://www.w3.org/TR/trig/

object RDFParser extends RDFParser

trait Chars {
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

trait RDFChars extends SimpleTypes{

  lazy val IRIREF_CHAR = P(!CharIn("<>\"{}|^`\\") ~CharIn('\u0021' to '\uFFFE'))

  lazy val unicodeEscape = P( "\\u" ~ HEX ~ HEX ~ HEX ~ HEX )

  lazy val unicodeEscapeLarge = P( "\\U" ~ HEX ~ HEX ~ HEX ~ HEX ~ HEX ~ HEX ~ HEX ~ HEX)

  lazy val UCHAR = P( unicodeEscape | unicodeEscapeLarge) //NOTE - check

  lazy val ECHAR = P("\\" ~ CharIn("tbnrf\"\'\\\\"))

  lazy val LOCAL_ESC = CharIn("_,~.-!$&'()*+,;=/?#@%")

  lazy val PN_LOCAL_ESC = P("\\" ~ LOCAL_ESC)

  lazy val PN_CHARS_BASE = P(
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

trait RDFParser extends RDFChars {


  lazy val PLX = P(PERCENT | PN_LOCAL_ESC)


  //[140s]	PNAME_LN	::=	PNAME_NS PN_LOCAL
  lazy val PNAME_LN = P(PNAME_NS ~ PN_LOCAL)

  //[139s]	PNAME_NS	::=	PN_PREFIX? ':'

  //[167s]	PN_PREFIX	::=	PN_CHARS_BASE ((PN_CHARS | '.')* PN_CHARS)?
  //lazy val PN_PREFIX = P(PN_CHARS_BASE ~ ((PN_CHARS | ".").rep ~PN_CHARS).?)
  //atomic(capture(PN_CHARS_BASE ~ (PN_CHARS | &(ch('.').+ ~ PN_CHARS) ~ ch('.').+ ~ PN_CHARS | isHighSurrogate ~ isLowSurrogate).*)) ~> ASTPNPrefix

  lazy val PN_PREFIX = P(PN_CHARS_BASE ~ (PN_CHARS | (&(".".rep(1) ~ PN_CHARS) ~ ".".rep(1) ~ PN_CHARS) | isHighSurrogate ~ isLowSurrogate).rep)


  lazy val PNAME_NS = P(PN_PREFIX.? ~ ":")
  //lazy val PNAME_NS  = P( PN_CHARS_BASE ~ (PN_CHARS | ".".rep ~ PN_CHARS) ~ ".".rep ~ PN_CHARS | isHighSurrogate ~ isLowSurrogate).rep)

  //[168s]	PN_LOCAL	::=	(PN_CHARS_U | ':' | [0-9] | PLX) ((PN_CHARS | '.' | ':' | PLX)* (PN_CHARS | ':' | PLX))?
  //lazy val PN_LOCAL = P( (PN_CHARS_U | ":" | DIGIT | PLX) ~ ((PN_CHARS | "." | ":" | PLX).rep(1) ~ (PN_CHARS | ":" | PLX)).rep)
  //(PLX | PN_CHARS_U_COLON_DIGIT ~ appendSB) ~ (PLX | PN_CHARS_COLON ~ appendSB | &(ch('.').+ ~ PN_CHARS_COLON) ~ (ch('.') ~ appendSB).+ ~ PN_CHARS_COLON ~ appendSB | isHighSurrogate ~ appendSB ~ isLowSurrogate ~ appendSB).*) ~ push(sb.toString) ~> ASTPNLocal

  lazy val PN_LOCAL = P((PLX | PN_CHARS_U_COLON_DIGIT) ~ (PLX | PN_CHARS_COLON | (&(".".rep(1) ~ PN_CHARS_COLON) ~ ".")).rep(1) ~ (PN_CHARS_COLON | (isHighSurrogate ~ isLowSurrogate)).rep)

  //lazy val PrefixedName = P( PNAME_NS  | PN_LOCAL )

  lazy val PrefixedName = P((PNAME_LN | PNAME_NS).!)

  //[135s]	iri	::=	IRIREF | PrefixedName
  lazy val IRI = P(IRIREF | PrefixedName)

  lazy val LANGTAG = P("@" ~ ALPHA.rep ~ ("-"+ ALPHANUMERIC.rep(1)).rep(0))

  lazy val IRIREF = P("<" ~ ((IRIREF_CHAR | UCHAR).rep).! ~ ">")

  lazy val EOL = CharIn("\n\r") //NOTE: test if works //alsof

  lazy val STRING_LITERAL_QUOTE = P("\"" ~ ( "^" | "#x22" | "#x5C" | "#xA" | "#xD" | ECHAR | UCHAR ).rep ~ "\"") //have no clue if [^#x22#x5C#xA#xD] should work

  lazy val BLANK_NODE_LABEL = P("_:" ~ (PN_CHARS_U | DIGIT) ~ (PN_CHARS | ".").rep ~ PN_CHARS)



  /*
  //[136s] PrefixedName 	::= 	PNAME_LN | PNAME_NS
  def prefixedName = rule {
    (PNAME_LN | PNAME_NS) ~> ASTPrefixedName
  }

  //[139s] PNAME_NS 	::= 	PN_PREFIX? ':'
  def PNAME_NS = rule {
    PN_PREFIX.? ~> ASTPNameNS ~ ':'
  }

  //[140s] PNAME_LN 	::= 	PNAME_NS PN_LOCAL
  def PNAME_LN = rule {
    PNAME_NS ~ PN_LOCAL ~> ((ns: ASTPNameNS, local: ASTPNLocal) ⇒ (test(addPrefix(ns, local)) | fail("name space - PNAME_NS=\"" + ns + "\" might be undefined")) ~ push(ns) ~ push(local)) ~> ASTPNameLN
  }

  //[167s] N_PREFIX 	::= 	PN_CHARS_BASE ((PN_CHARS | '.')* PN_CHARS)?
  /* A prefix name may not start or end with a '.' (DOT), but is allowed to have any number of '.' in between.
	 The predicate "&(DOT.+ ~ PN_CHARS)", looks ahead and checks if the rule in braces will be fullfilled.
	 It does so without interfering with the parsing process.
	 Example:
	 [] <b> c:d.1..2...3.
	 Due to the predicate the last '.' is not part of the local name. The accepted name is "c:d.1..2...3",
	 with the last '.' being recognized as triple terminator.
	 */
  def PN_PREFIX = rule {
    atomic(capture(PN_CHARS_BASE ~ (PN_CHARS | &(ch('.').+ ~ PN_CHARS) ~ ch('.').+ ~ PN_CHARS | isHighSurrogate ~ isLowSurrogate).*)) ~> ASTPNPrefix
  }

  //[168s] PN_LOCAL 	::= 	(PN_CHARS_U | ':' | [0-9] | PLX) ((PN_CHARS | '.' | ':' | PLX)* (PN_CHARS | ':' | PLX))?
  /* A local name may not start or end with a '.' (DOT), but is allowed to have any number of '.' in between.
	 The predicate "&(DOT.+ ~ PN_CHARS_COLON)", looks ahead and checks if the rule in braces will be fullfilled.
	 It does so without interfering with the parsing process.
	 Example:
	 [] <b> c:d.1..2...3.
	 Due to the predicate the last '.' is not part of the local name. The accepted name is "c:d.1..2...3",
	 with the last '.' being recognized as triple terminator.
	 */
  def PN_LOCAL = rule {
    clearSB ~ atomic((PLX | PN_CHARS_U_COLON_DIGIT ~ appendSB) ~ (PLX | PN_CHARS_COLON ~ appendSB | &(ch('.').+ ~ PN_CHARS_COLON) ~ (ch('.') ~ appendSB).+ ~ PN_CHARS_COLON ~ appendSB | isHighSurrogate ~ appendSB ~ isLowSurrogate ~ appendSB).*) ~ push(sb.toString) ~> ASTPNLocal
  }
  */

  //lazy val PN_LOCAL = P()
}
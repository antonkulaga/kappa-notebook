package org.denigma.kappa.parsers

import fastparse.all._
//https://www.w3.org/TR/trig/

object RDFParser extends RDFParser

trait ExtRDFParser extends RDFParser {

  lazy val IRI_lite = P("<".? ~ ((IRIREF_CHAR | UCHAR).rep).! ~ ">".? | PrefixedName) //just to ease the restrictions

  lazy val comment = P( PNAME_NS ~ ("comment" | "has_comment") ~ spaces ~ STRING_LITERAL_QUOTE)

}

trait RDFParser extends RDFCharsParser {


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

  //lazy val STRING_LITERAL_QUOTE = P("\"" ~ ( "^" | "#x22" | "#x5C" | "#xA" | "#xD" | ECHAR | UCHAR ).rep ~ "\"") //have no clue if [^#x22#x5C#xA#xD] should work

  lazy val BLANK_NODE_LABEL = P("_:" ~ (PN_CHARS_U | DIGIT) ~ (PN_CHARS | ".").rep ~ PN_CHARS)


  //lazy val STRING_LITERAL = P(STRING_LITERAL_LONG_SINGLE_QUOTE | STRING_LITERAL_LONG_QUOTE | STRING_LITERAL_QUOTE | STRING_LITERAL_SINGLE_QUOTE)


  //[22] STRING_LITERAL_QUOTE   ::=     '"' ([^#x22#x5C#xA#xD] | ECHAR | UCHAR)* '"' /* #x22=" #x5C=\ #xA=new line #xD=carriage return */
  lazy val STRING_LITERAL_QUOTE = P( "\"" ~ ((!CharIn("\"\\\\\r\n") ~ AnyChar) | (UCHAR | ECHAR)).rep.! ~ "\"")

  /*
  //[23] '" ([^#x27#x5C#xA#xD] | ECHAR | UCHAR)* "'" /* #x27=' #x5C=\ #xA=new line #xD=carriage return */
  def STRING_LITERAL_SINGLE_QUOTE = rule {
    '\'' ~ clearSB ~ (noneOf("'\"\\\r\n") ~ appendSB | '"' ~ appendSB("\\\"") | UCHAR(true) | ECHAR).* ~ '\'' ~ push(sb.toString) ~> ASTStringLiteralSingleQuote
  }

  //[24] STRING_LITERAL_LONG_SINGLE_QUOTE       ::=     "'''" (("'" | "''")? ([^'\] | ECHAR | UCHAR))* "'''"
  def STRING_LITERAL_LONG_SINGLE_QUOTE = rule {
    str("'''") ~ clearSB ~ (capture(('\'' ~ '\'' ~ !'\'' | '\'' ~ !('\'' ~ '\'')).?) ~> ((s: String) ⇒ appendSB(s.replaceAllLiterally("\"", "\\\""))) ~ (capture(noneOf("\'\\\"")) ~> ((s: String) ⇒ run(maskEsc(s))) | '"' ~ appendSB("\\\"") | UCHAR(true) | ECHAR)).* ~ str("'''") ~ push(sb.toString) ~> ASTStringLiteralLongSingleQuote
  }

  //[25] STRING_LITERAL_LONG_QUOTE      ::=     '"""' (('"' | '""')? ([^"\] | ECHAR | UCHAR))* '"""'
  def STRING_LITERAL_LONG_QUOTE = rule {
    str("\"\"\"") ~ clearSB ~ (capture(('"' ~ '"' ~ !'"' | '"' ~ !('"' ~ '"')).?) ~> ((s: String) ⇒ appendSB(s.replaceAllLiterally("\"", "\\\""))) ~ (capture(noneOf("\"\\")) ~> ((s: String) ⇒ run(maskEsc(s))) | UCHAR(true) | ECHAR)).* ~ str("\"\"\"") ~ push(sb.toString) ~> ASTStringLiteralLongQuote
  }
  */


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
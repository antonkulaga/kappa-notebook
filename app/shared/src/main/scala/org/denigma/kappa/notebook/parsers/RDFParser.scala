package org.denigma.kappa.notebook.parsers

import fastparse.all
import fastparse.all._

/**
  * Created by antonkulaga on 06/03/16.
  */
class RDFParser {

  //https://github.com/sirthias/parboiled/blob/master/examples-java/src/main/java/org/parboiled/examples/sparql/SparqlParser.java

  val DIGIT = P(CharIn('0' to '9'))


  val PN_CHARS_BASE = P(
    CharIn('A' to 'Z') |  CharIn('a' to 'z') |
      CharIn('\u00C0' to '\u00D6') |  CharIn('\u00D8' to '\u00F6') |
      CharIn('\u00F8' to '\u02FF') |  CharIn('\u0370' to '\u037D') |
      CharIn('\u037F' to '\u1FFF') |  CharIn('\u200C' to '\u200D') |
      CharIn('\u2070' to '\u218F') |  CharIn('\u2C00' to '\u2FEF') |
      CharIn('\u3001' to '\uD7FF') |  CharIn('\uF900' to '\uFDCF') |
      CharIn('\uFDF0' to '\uFFFD')
  )

  val EOL = CharIn("\n\r")

  //def NOT

  val NOT_EOL = P(!EOL.flatMap(v => AnyChar)) //BAD PRACTISE

  val COMMENT = P("#" ~ (NOT_EOL ~ AnyChar).rep ~ EOL  )

  val WS_NO_COMMENT = P(" " | "\n" | "\t" | "\f" | EOL)

  val LESS_NO_COMMENT = P("<" ~ WS_NO_COMMENT.rep)

  val WS = P((COMMENT | WS_NO_COMMENT).rep)

  val ECHAR = P("\\" ~ CharIn("tbnrf\\\"\'"))

  val NF = P(!CharIn("\"\\\n\r")).flatMap(v => AnyChar) //TestNot(AnyOf("\"\\\n\r")

  val STRING_LITERAL1 = P("'" ~ ((NF ~ AnyChar) | ECHAR).rep ~ "'"~ WS ) //"

  val STRING_LITERAL2 = P("\"" ~ ((NF ~ AnyChar) | ECHAR).rep ~ "\""~ WS ) //'

  val NF_S = P(!CharIn("\'\\")).flatMap(v => AnyChar) //"'", "\\"

  val STRING_LITERAL1_LONG = P("'''" ~  (("''" | "'").? ~ ((NF_Q ~ AnyChar) | ECHAR) ).rep ~ "'''"~ WS ) //"

  val NF_Q = P(!CharIn("\"\\")).flatMap(v => AnyChar) //"'", "\\"

  val STRING_LITERAL2_LONG = P("'''" ~  (("''" | "'").? ~ ((NF_S ~ AnyChar) | ECHAR) ).rep ~ "'''"~ WS ) //"

  val STRING = P(STRING_LITERAL1 | STRING_LITERAL2 | STRING_LITERAL1_LONG | STRING_LITERAL2_LONG)

  val STRING_WS = P(STRING | WS)

  //val IRI_REF = P()
/*
  public Rule IRI_REF() {
    return Sequence(LESS_NO_COMMENT(), //
      ZeroOrMore(Sequence(TestNot(FirstOf(LESS_NO_COMMENT(), GREATER(), '"', OPEN_CURLY_BRACE(),
        CLOSE_CURLY_BRACE(), '|', '^', '\\', '`', CharRange('\u0000', '\u0020'))), ANY)), //
      GREATER());
  }
  */

}
/*


  val DIGIT = P(CharIn('0' to '9'))

  val HEX = P(CharIn('0' to '9') | CharIn('A' to 'F') | CharIn('a' to 'f'))

  val PN_CHARS_BASE = P(
    CharIn('A' to 'Z') |  CharIn('a' to 'z') |
    CharIn('\u00C0' to '\u00D6') |  CharIn('\u00D8' to '\u00F6') |
    CharIn('\u00F8' to '\u02FF') |  CharIn('\u0370' to '\u037D') |
    CharIn('\u037F' to '\u1FFF') |  CharIn('\u200C' to '\u200D') |
    CharIn('\u2070' to '\u218F') |  CharIn('\u2C00' to '\u2FEF') |
    CharIn('\u3001' to '\uD7FF') |  CharIn('\uF900' to '\uFDCF') |
    CharIn('\uFDF0' to '\uFFFD')
  )
  val PN_CHARS_U = P(PN_CHARS_BASE | "_" )

  val PN_CHARS = P("-" | DIGIT | PN_CHARS_U | "\u00B7" | CharIn('\u0300'  to '\u036F') | CharIn ('\u203F' to '\u2040'))

  val ECHAR = P("\\" ~ CharIn("tbnrf\\\"\'"))

  val unicodeEscape = P( "u" ~ HEX ~ HEX ~ HEX ~ HEX )

  val unicodeEscapeLarge = P( "U" ~ HEX ~ HEX ~ HEX ~ HEX ~ HEX ~ HEX )

  val UCHAR = P( unicodeEscape | unicodeEscapeLarge) //NOTE - check

  /*public Rule PN_LOCAL() {
    return Sequence(FirstOf(PN_CHARS_U(), DIGIT()),
      Optional(ZeroOrMore(FirstOf(PN_CHARS(), Sequence(DOT(), PN_CHARS())))), WS());
  }
  */

  //val PN_LOCAL = P( (PN_CHARS_U | DIGIT) ~ (PN_CHARS | ("." ~ PN_CHARS)))

  val BLANK_NODE_LABEL = P("_:" ~ (PN_CHARS_U | DIGIT) ~ (PN_CHARS | ".").rep ~ PN_CHARS)

  val STRING_LITERAL_QUOTE = P("\"" ~ ( "\^" | "#x22" | "#x5C" | "#xA" | "#xD" | ECHAR | UCHAR ).rep ~ "\"") //have no clue if [^#x22#x5C#xA#xD] should work

  val IRI_REF = ""
 */
/*
[1] 	ntriplesDoc 	::= 	triple? (EOL triple)* EOL?
[2] 	triple 	::= 	subject predicate object '.'
[3] 	subject 	::= 	IRIREF | BLANK_NODE_LABEL
[4] 	predicate 	::= 	IRIREF
[5] 	object 	::= 	IRIREF | BLANK_NODE_LABEL | literal
[6] 	literal 	::= 	STRING_LITERAL_QUOTE ('^^' IRIREF | LANGTAG)?


[144s]	LANGTAG	::=	'@' [a-zA-Z]+ ('-' [a-zA-Z0-9]+)*
[7]	EOL	::=	[#xD#xA]+
[8]	IRIREF	::=	'<' ([^#x00-#x20<>"{}|^`\] | UCHAR)* '>'
-+[9]	STRING_LITERAL_QUOTE	::=	'"' ([^#x22#x5C#xA#xD] | ECHAR | UCHAR)* '"'
+[141s]	BLANK_NODE_LABEL	::=	'_:' (PN_CHARS_U | [0-9]) ((PN_CHARS | '.')* PN_CHARS)?
+[153s]	ECHAR	::=	'\' [tbnrf"'\]
+[157s]	PN_CHARS_BASE	::=	[A-Z] | [a-z] | [#x00C0-#x00D6] | [#x00D8-#x00F6] | [#x00F8-#x02FF] | [#x0370-#x037D] | [#x037F-#x1FFF] | [#x200C-#x200D] | [#x2070-#x218F] | [#x2C00-#x2FEF] | [#x3001-#xD7FF] | [#xF900-#xFDCF] | [#xFDF0-#xFFFD] | [#x10000-#xEFFFF]
+[158s]	PN_CHARS_U	::=	PN_CHARS_BASE | '_' | ':'
+[160s]	PN_CHARS	::=	PN_CHARS_U | '-' | [0-9] | #x00B7 | [#x0300-#x036F] | [#x203F-#x2040]
+[162s]	HEX	::=	[0-9] | [A-F] | [a-f]
* */


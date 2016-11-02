object Versions extends WebJarsVersions
	with ScalaJSVersions
	with SharedVersions
	with Binding
	with OtherJVM
{

	val scala = "2.11.8"

	val kappaNotebook = "0.0.17"

	val kappaModel = "0.0.2"

	val websim = "0.0.17"


}

trait Binding {

	val binding = "0.8.15"

	val bindingControls = "0.0.22"

	val semanticControls = bindingControls
}

trait OtherJVM {

	val bcrypt = "2.4"

	val ammonite = "0.7.8"

	val apacheCodec = "1.10"

	val akkaHttpExtensions = "0.0.14"

	val retry = "0.2.1"

	val logback = "1.1.7"

	val akka = "2.4.11"

	val circeHttp = "1.10.1"

	val libSBOLj = "2.1.0"

	val breeze = "0.12"

	val ficus: String = "1.3.0"

	val paxtools = "4.3.1"

	val scalaJSscripts = "1.0.0"

	val betterFiles = "2.16.0"

}


trait ScalaJSVersions {

	val dom = "0.9.1"

	val codemirrorFacade = "5.13.2-0.7"

	val threejsFacade = "0.0.74-0.1.7"

	val d3jsFacade = "0.3.3"

	val pdfJSFacade = "0.8.0-0.0.5"

	val jsext = "0.7"
}

//versions for libs that are shared between client and server
trait SharedVersions
{

	var sourcecode = "0.1.3"

	val annotator = "0.0.5"

	val circe = "0.5.4"

	val scalaTags = "0.6.0"

	val scalaCSS = "0.5.0"

	val scalaTest = "3.0.0"

	val scalaCheck = "1.13.4"

	val fastparse = "0.4.2"

	val quicklens = "1.4.8"

	val booPickle = "1.2.4"

	val pprint: String ="0.4.3"

	val roshttp = "1.1.0"

	val banana = "0.8.3-SNAP1"

	val javaTime = "0.2.0"

	val simulacrum = "0.10.0"

	val paradise = "2.1.0"

}


trait WebJarsVersions{

	val jquery =  "2.2.4"//"3.0.0"

	val jquerySVG = "1.5.0"

	val semanticUI = "2.2.2"

	val codemirror = "5.13.2"

	val threeJS = "r77"

	val webcomponents = "0.7.12"

	val d3js = "3.5.17"

	val malihuScrollBar: String = "3.1.5"

	val scrollTo = "2.1.1"

}


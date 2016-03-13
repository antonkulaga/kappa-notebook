object Versions extends WebJarsVersions with ScalaJSVersions with SharedVersions
{
	val scala = "2.11.8"

	val akkaHttp = "2.4.2"

	val bcrypt = "2.4"

	val ammonite = "0.5.6"

	val apacheCodec = "1.10"

	val akkaHttpExtensions = "0.0.10"

	val binding = "0.8.3"

	val bindingControls = "0.0.12"

	val kappaNotebook = "0.0.6"

	val retry = "0.2.1"

	val macroParadise = "2.1.0"

	val logback = "1.1.6"

	val akka = "2.4.2"

	val circeHttp = "1.5.2"

	val libSBOLj = "2.0.0"

}

trait ScalaJSVersions {

	val jqueryFacade = "0.11" //1.0-RC2

	val semanticUIFacade = "0.0.1"

	val dom = "0.9.0"

	val codemirrorFacade = "5.11-0.7"

	val threejsFacade = "0.0.74-0.1.6"

	val d3jsFacade = "0.3.1"

}

//versions for libs that are shared between client and server
trait SharedVersions
{

	val circe = "0.3.0"

	val scalaTags = "0.5.4"

	val scalaCSS = "0.4.0"

	val productCollections = "1.4.2"

	val scalaTest = "3.0.0-SNAP13"

	val fastparse = "0.3.7"

}


trait WebJarsVersions{

	val jquery =  "2.2.1"

	val semanticUI = "2.1.8"

	val codemirror = "5.12"

	val threeJS = "r74"

	val webcomponents = "0.7.12"

	val d3js: String = "3.5.12"


}


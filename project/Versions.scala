object Versions extends WebJarsVersions with ScalaJSVersions with SharedVersions
{
	val scala = "2.11.7"

	val akkaHttp = "2.4.2"

	val bcrypt = "2.4"

	val ammonite = "0.5.5"

	val apacheCodec = "1.10"

	val akkaHttpExtensions = "0.0.10"

	val bindingControls = "0.0.12"

	val kappaNotebook = "0.0.5"

	val retry = "0.2.1"

	val threejsFacade = "0.0.71-0.1.5"

	val macroParadise = "2.1.0"

	val logback = "1.1.6"

	val akka = "2.4.2"

	val circeHttp = "1.5.2"

}

trait ScalaJSVersions {

	val jqueryFacade = "0.11"

	val semanticUIFacade = "0.0.1"

	val dom = "0.9.0"

	val codemirrorFacade = "5.11-0.7"

	val binding = "0.8.3"

}

//versions for libs that are shared between client and server
trait SharedVersions
{

	val circe = "0.3.0"

	val scalaTags = "0.5.4"

	val scalaCSS = "0.4.0"

	val productCollections = "1.4.2"

	val scalaTest = "3.0.0-SNAP13"

	val fastparse = "0.3.6"

}


trait WebJarsVersions{

	val jquery =  "2.2.1"

	val semanticUI = "2.1.8"

	val codemirror = "5.11"

	val threeJS = "r74"

	val webcomponents = "0.7.12"

}


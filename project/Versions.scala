object Versions extends WebJarsVersions with ScalaJSVersions with SharedVersions
{
	val scala = "2.11.7"

	val akkaHttp = "2.0.1"

	val bcrypt = "2.4"

	val ammonite = "0.5.2"

	val apacheCodec = "1.10"

	val akkaHttpExtensions = "0.0.9-M2"

	val bindingControls = "0.0.9-M3"

	val kappaNotebook = "0.0.3"

	val retry = "0.2.1"

	val threejsFacade = "0.0.71-0.1.5"

	val macroParadise = "2.1.0-M5"

}

trait ScalaJSVersions {

	val jqueryFacade = "0.10"

	val semanticUIFacade = "0.0.1"

	val dom = "0.8.2"

	val codemirrorFacade = "5.4-0.5"

	val binding = "0.8.1-M3"

}

//versions for libs that are shared between client and server
trait SharedVersions
{
	val scalaRx = "0.2.8"

	val scalaTags = "0.5.2"

	val scalaCSS = "0.3.2"

	val productCollections = "1.4.2"

	val scalaTest = "3.0.0-SNAP13"

	val fastparse = "0.3.4"

}


trait WebJarsVersions{

	val jquery =  "2.1.4"

	val semanticUI = "2.1.6"

	val codemirror = "5.5"

	val threeJS = "r71"

	val webcomponents = "0.7.7"

}


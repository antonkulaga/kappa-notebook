Kappa notebook
##############

[![Gitter chat channel](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/denigma/denigma-libs?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)

Kappa notebook is a webapplication that::
* runs kappa models and displays the results.
* visualizes rules
* supports annotations from papers, pictures, videos and links
* displays contact-maps
* allows to work with multiple kappa-projects
 
In plans::
* Fluxes visualizations
* Generating kappa models from plasmid-maps in SBOL format.
* Collaborative editing of models
* Static Analysis output
* Basic NLP to detect entities inside papers
* Management of connections to multiple WebSim servers
* other improvements

Kappa is a rule-based language for modeling protein networks.

You can try Kappa-notebook at [http://kappa.antonkulaga.name](http://kappa.antonkulaga.name) or build it from sources. 

Note #1: sometimes it can be broken as I use this address as a playground.

Note #2: It is recommended to use it from Chrome. Internet Explorer is not supported

![Screenshot](/screenshot.jpg?raw=true "Kappa-notebook screenshot")


Using WebSim scala API
----------------------

The server version of Kappa simulator is called WebSim. Scala API to interact with WebSim is included in this repository
and situated inside [websim subproject](/websim), it also contains some integration tests so you can check if it works with your version of WebSim.
To add WebSim API to your Scala project add following dependencies to your sbt configuration:
```sbt
resolvers += sbt.Resolver.bintrayRepo("denigma", "denigma-releases")
libraryDependencies += "org.denigma" %% "websim" % "0.0.15"
```
It also contains some shared (ScalaJVM/ScalaJS) case classes that can be used to exchange messages between client and server part.

Most of the code is located inside [WebSimClient class](/websim/jvm/src/main/scala/org/denigma/kappa/notebook/services/WebSimClient.scala)

The API is mostly akka-streams oriented. [Here](/app/jvm/src/main/scala/org/denigma/kappa/notebook/communication/KappaServerActor.scala) is how it is used inside kappa-notebook.

Running prebuilt binary
-----------------------

Get a latest zip from https://github.com/antonkulaga/kappa-notebook/releases, unpack it and run `bin/kappa-notebook-root`.
That will start notebook on http://localhost:1234

Building from source and running examples
-----------------------------------------

To clone the repository and initialize sumbmodule with example models:
```bash
git clone https://github.com/antonkulaga/kappa-notebook
git submodule init
git submodule update
```
To build from source:
```
Install [sbt](http://www.scala-sbt.org/)
Make sure that [KaSim Server](https://github.com/Kappa-Dev/KaSim) is up and running in your system (see instructions of building it from source below)
Type the following commands:
```bash
$ sbt // to open sbt console
$ re-start // will open akka-http application with examples
```
It will open a local version of kappa-notebook at http://localhost:1234/ 
If you have a debian-based Linux you can also run sbt _debian:packageBin_ command to create a deb installer in the _target_ folder


Building WebSim server
----------------------

In order to work Kappa-notebook has to be connected to a WebSim server. 
WebSim server is a version of [Kappa Simulation (KaSim)](https://github.com/Kappa-Dev/KaSim) with REST API.
There is a [nightly build](http://www.kappalanguage.org/nightly-builds/WebSim_master_x86_64_ubuntu16.04) of WebSim for Ubuntu,
if you have another OS, you have to build it yourself.

Right now there is no public release of WebSim so you have to build it yourself.

To build WebSim for Ubuntu/Mint and other Debian based distributions:

```bash
    #install opam by:
    wget https://raw.github.com/ocaml/opam/master/shell/opam_installer.sh -O - | sh -s /usr/local/bin
    opam init #init opam
    opam install yojson atdgen lwt cohttp depext #install main dependencies
    opam depext conf-ncurses.1 #do it f there will be problems with ncurses and run installation of dependencies after it
```

 * `git clone git@github.com:Kappa-Dev/KaSim.git`
 * in KaSim folder run `make WebSim.native`
 * run `./WebSim.native`. That will start server on default port 8080
 * test it by opening http://localhost:8080/v1/version in your browser
    
Note: sometimes building can be tricky. For instance in Ubuntu-based distros the most common problem is when ocaml is installed from ubuntu/debian ocaml package and it does not see dependencies installed by opam.
That is why I recommend to install binary version of opam and from it - install everything else.
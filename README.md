Kappa notebook
#######################
[![Gitter chat channel](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/denigma/denigma-libs?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)

This project is a webinterface that runs kappa models and displays the results.
It also visualizes rules and in the nearest Future will support semantic annotations and collaborative work on the models.
[Kappa](http://dev.executableknowledge.org/) is a rule-based language for modeling protein networks.

You can try Kappa-notebook at [http://kappa.antonkulaga.name](http://kappa.antonkulaga.name) or build it from sources. 
Note: sometimes it can be broken as I use this address as a playground.

Building from source and running examples
-----------------------------------------

To build from source:
Install [sbt](http://www.scala-sbt.org/)
Make sure that [KaSim Server](https://github.com/Kappa-Dev/KaSim) is up and running in your system
Type the following commands:
```scala
$ sbt // to open sbt console
$ re-start // will open akka-http application with examples
```
It will open a local version of kappa-notebook at http://localhost:1234/ 
If you have a debian-based Linux you can also run sbt _debian:packageBin_ command to create a deb installer in the _target_ folder

Building WebSim server
----------------------

In order to work Kappa-notebook has to be connected to a WebSim server. 
WebSim server is a version of [Kappa Simulation (KaSim)](https://github.com/Kappa-Dev/KaSim) with REST API.
Right now there is not public release of WebSim so you have to build it yourself.

In order to make it work, you need to :
    * install opam : http://opam.ocaml.org/doc/Install.html
    * install the dependency via the command ‘opam install yojson atdgen lwt cohttp’
    * go into KaSim folder up to date in master branch and invoke ‘make WebSim.native’
    * ./WebSim.native launch it on port 8080
    *   test it by asking http://localhost:8080/v1/version in your browser
    
Note: sometimes building can be tricky. For instance the most common problem is when ocaml does not see dependencies installed by opam.
That is why it is better to install opam from binaries ( http://opam.ocaml.org/doc/Install.html ) then from packages.
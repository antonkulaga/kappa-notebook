Kappa notebook
##############

[![Gitter chat channel](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/denigma/denigma-libs?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)

This project is a webinterface that runs kappa models and displays the results.
It also visualizes rules and in the nearest Future will support semantic annotations and collaborative work on the models.
[Kappa](http://dev.executableknowledge.org/) is a rule-based language for modeling protein networks.

You can try Kappa-notebook at [http://kappa.antonkulaga.name](http://kappa.antonkulaga.name) or build it from sources. 
Note: sometimes it can be broken as I use this address as a playground.

Building from source and running examples
-----------------------------------------

*WARNING*: I AM ADOPTING TO NEW API SO LATEST COMMIT DOES NOT COMPILE!!!

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

Using WebSim scala API
----------------------

Scala API for dealing with WebSim (Kappa server) is now separated into websim subproject and published as a separate library so you can use it to build your own Scala client.

Building WebSim server
----------------------

In order to work Kappa-notebook has to be connected to a WebSim server. 
WebSim server is a version of [Kappa Simulation (KaSim)](https://github.com/Kappa-Dev/KaSim) with REST API.
Right now there is not public release of WebSim so you have to build it yourself.

To build WebSim for Ubuntu/Mint and other Debian based distributions:

    ```bash
        #install opam by:
        wget https://raw.github.com/ocaml/opam/master/shell/opam_installer.sh -O - | sh -s /usr/local/bin
        opam init #init opam
        opam install yojson atdgen lwt cohttp depext #install main dependencies
        opam depext conf-ncurses.1 #do it f there will be problems with ncurses and run installation of dependencies after it
    ```


    * go into KaSim folder up to date in master branch and invoke ‘make WebSim.native’
    * ./WebSim.native launch it on port 8080
    * test it by asking http://localhost:8080/v1/version in your browser
    
Note: sometimes building can be tricky. The most common problem is when ocaml is installed from ubuntu/debian ocaml package and it does not see dependencies installed by opam.
That is why I recommend to avoid installing ocaml from packages but to install binary version of opam and from it - install everything else.
Kappa notebook
#######################
[![Gitter chat channel](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/denigma/denigma-libs?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)

This project is a webinterface that runs kappa models and displays the results.
[Kappa](http://dev.executableknowledge.org/) is a rule-based language for modeling protein networks.

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

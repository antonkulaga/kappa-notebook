Kappa notebook
#######################
[![Gitter chat channel](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/denigma/denigma-libs?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)

This project is a webinterface that runs kappa models and displays the results.
[Kappa](http://dev.executableknowledge.org/) is a rule-based language for modeling protein networks.
Most of kappa simulations are done by KaSim software that is console based.

Kappa-notebook is just an effort to make KaSim experience better for the end user. 
It provides browser-based user interface and then runs KaSim with corresponding parameters.
At the moment console output and charts are supported. 

You can look at working version of this simulator at [demo page](http://kappa.antonkulaga.name) , you can also install one to your PC.
I also recommend to look at [javascript version of KaSim](http://dev.executableknowledge.org/try/index.html) that is slow but does not require any server to run. 

Building from source and running examples
-----------------------------------------

To build from source:
Install [sbt](http://www.scala-sbt.org/)
Make sure that [KaSim](https://github.com/Kappa-Dev/KaSim) is in your system path and can be run by "KaSim" command.
Type the following commands:
```scala
$ sbt // to open sbt console
$ re-start // will open akka-http application with examples
```
It will open a local version of kappa-notebook at http://localhost:1234/
If you have a debian-based Linux you can also run sbt _debian:packageBin_ command to create a deb installer in the _target_ folder
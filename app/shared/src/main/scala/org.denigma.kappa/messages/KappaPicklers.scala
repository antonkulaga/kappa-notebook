package org.denigma.kappa.messages

import boopickle.Default._


trait KappaPicklers {

  implicit val datePickler = transformPickler[java.util.Date, Long](_.getTime, t => new java.util.Date(t))
  //implicit val dateTimePickler = transformPickler[LocalDateTime, Long](_., t => new java.util.Date(t))



  object Single{
/*
    implicit val kappaPathPickler = compositePickler[KappaPath]
        .addConcreteType[KappaFile]
        .addConcreteType[KappaFolder]
*/

    implicit val webSimMessagePickler = compositePickler[WebSimMessage]//compositePickle[WebSimMessage]
      .addConcreteType[RunModel]
      .addConcreteType[Observable]
      .addConcreteType[KappaPlot]
      .addConcreteType[FluxData]
      .addConcreteType[FluxMap]
      .addConcreteType[SimulationStatus]
      .addConcreteType[VersionInfo]


    implicit val kappaPathPickler = compositePickler[KappaPath]
        .addConcreteType[KappaFile]
        .addConcreteType[KappaFolder]


    implicit val kappaMessagePickler = compositePickler[KappaMessage]
      .addConcreteType[SimulationResult]
      .addConcreteType[SyntaxErrors]
      .addConcreteType[ServerErrors]
      .addConcreteType[Connected]
      .addConcreteType[Disconnected]
      .addConcreteType[KappaFile]
      .addConcreteType[KappaFolder]
      .addConcreteType[KappaProject]
      .addConcreteType[KappaUser]
      .addConcreteType[ServerConnection]
      .addConcreteType[ConnectedServers]

  }

  import Single._

  implicit val webSimMessagePickler = compositePickler[WebSimMessage]
    .addConcreteType[RunModel]
    .addConcreteType[Observable]
    .addConcreteType[KappaPlot]
    .addConcreteType[FluxData]
    .addConcreteType[FluxMap]
    .addConcreteType[SimulationStatus]
    .addConcreteType[VersionInfo]

  implicit val kappaMessagePickler = compositePickler[KappaMessage]
      //.addConcreteType[Code]
      .addConcreteType[Load]
      .addConcreteType[KappaProject]
      .addConcreteType[LaunchModel]
      .addConcreteType[SimulationResult]
      .addConcreteType[SyntaxErrors]
      .addConcreteType[ServerErrors]
      .addConcreteType[Connected]
      .addConcreteType[Disconnected]
      .addConcreteType[KappaFile]
      .addConcreteType[KappaFolder]
      .addConcreteType[Loaded]

}

package org.denigma.kappa.messages

import boopickle.Default._


trait KappaPicklers {

  //implicit val datePickler = transformPickler[java.util.Date, Long](_.getTime, t => new java.util.Date(t))
  //implicit val dateTimePickler = transformPickler[LocalDateTime, Long](_., t => new java.util.Date(t))
  
}

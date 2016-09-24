package org.denigma.kappa.messages

import org.denigma.binding.extensions.MapUpdate


object FilesUpdate{
  val SERVER = "SERVER"
  val UI = "UI"

  def updatedFiles(files: KappaFile*) = FilesUpdate(updated = files.map(f=> (f.path , f)).toMap)
}
case class FilesUpdate(added: Map[String, KappaFile]= Map.empty,
                       removed: Map[String, KappaFile] = Map.empty,
                       updated: Map[String, KappaFile] = Map.empty,
                       source: String = FilesUpdate.UI) extends UIMessage
{
  def nonEmpty = added.nonEmpty || removed.nonEmpty || updated.nonEmpty
}
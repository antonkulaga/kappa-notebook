package org.denigma.kappa.notebook

import better.files._
object extensions extends SharedExtensions{

  implicit class FileExt(val file: File) {

    def child(str: String): File = child(str, file)

    protected def child(str: String, f: File):File = f.parentOption match {
      case None => f / str
      case Some(p) if (p / str).isChildOf(file) => child(str, p)
      case _ => f / str
    }

    def resolveChild(path: String, mustExist: Boolean = false): Option[File] = {
      child(path, file) match {
        case child if child.isChildOf(file) =>
          if(!mustExist || child.exists) Some(child) else None
        case _ => None
      }
    }

    def resolveChild(path: String, subPath: String, mustExist: Boolean): Option[File] = resolveChild(path, mustExist).flatMap(r => r.resolveChild(subPath, mustExist))

  }

}

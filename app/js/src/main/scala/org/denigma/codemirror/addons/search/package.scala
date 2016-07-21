package org.denigma.codemirror.addons

import org.denigma.codemirror.{CodeMirror, EditorConfiguration}

/**
  * Created by antonkulaga on 7/21/16.
  */
package object search {


    implicit def extendedConfiguration(config: EditorConfiguration): SearchConfiguration =
      config.asInstanceOf[SearchConfiguration]

    implicit class CodeMirrorSearchable(obj: CodeMirror.type) {

    }

}

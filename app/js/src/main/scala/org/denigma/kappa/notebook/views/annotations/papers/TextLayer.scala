package org.denigma.kappa.notebook.views.annotations.papers

import org.denigma.controls.pdf.{TextContent, PDFRenderTask, PDFJS, PDFPageViewport}
import org.scalajs.dom
import org.scalajs.dom.CustomEvent
import org.denigma.binding.extensions._
import scala.scalajs.js
import scala.scalajs.js.JSConverters._

/**
  * Created by antonkulaga on 02/06/16.
  */
class TextBuilder(val textLayerDiv: dom.Element, val pageIndex: Int, val viewport: PDFPageViewport) {
  val textDivs = new js.Array[org.scalajs.dom.html.Div]()
  val pageNumber = pageIndex + 1
  var textLayerRenderTask: PDFRenderTask = null //terrible
  var renderingDone = false
  var divContentDone = false

  def render(textContent: TextContent) = {
    val items = textContent.items.toList
    println("items = ")
    items.foreach(i=>println(i.dir))
    items.foreach(i=>println(i.str))
    println("styles = ")
    val styles = textContent.styles
    println(styles)

    PDFJS.dyn.renderTextLayer()

  }

  /*
  *
  * _render: function TextLayer_render(timeout) {
      var textItems = this._textContent.items;
      var styles = this._textContent.styles;
      var textDivs = this._textDivs;
      var viewport = this._viewport;
      for (var i = 0, len = textItems.length; i < len; i++) {
        appendText(textDivs, viewport, textItems[i], styles);
      }

      if (!timeout) { // Render right away
        render(this);
      } else { // Schedule
        var self = this;
        this._renderTimer = setTimeout(function() {
          render(self);
          self._renderTimer = null;
        }, timeout);
      }
    }
  };
  * */

  /*
  /**
    * Renders the text layer.
    * @param {number} timeout (optional) if specified, the rendering waits
    *   for specified amount of ms.
    */
  render: function TextLayerBuilder_render(timeout) {
    if (!this.divContentDone || this.renderingDone) {
    console.error("it is done!");
    return;
  }

    if (this.textLayerRenderTask) {
    this.textLayerRenderTask.cancel();
    this.textLayerRenderTask = null;
  }

    this.textDivs = [];
    var textLayerFrag = document.createDocumentFragment();
    this.textLayerRenderTask = PDFJS.renderTextLayer({
    textContent: this.textContent,
    container: textLayerFrag,
    viewport: this.viewport,
    textDivs: this.textDivs,
    timeout: timeout
  });
    this.textLayerRenderTask.promise.then(function () {
    this.textLayerDiv.appendChild(textLayerFrag);
    this._finishRendering();
    this.updateMatches();
  }.bind(this), function (reason) {
    console.error("SOME FAILURE")
    // canceled or failed to render text layer -- skipping errors
  });
  },
  */

  /*
  *
  *    /**
         * Fixes text selection: adds additional div where mouse was clicked.
         * This reduces flickering of the content if mouse slowly dragged down/up.
         * @private
         */
        _bindMouse: function TextLayerBuilder_bindMouse() {
            var div = this.textLayerDiv;
            div.addEventListener('mousedown', function (e) {
                var end = div.querySelector('.endOfContent');
                if (!end) {
                    return;
                }
//#if !(MOZCENTRAL || FIREFOX)
                // On non-Firefox browsers, the selection will feel better if the height
                // of the endOfContent div will be adjusted to start at mouse click
                // location -- this will avoid flickering when selections moves up.
                // However it does not work when selection started on empty space.
                var adjustTop = e.target !== div;
//#if GENERIC
                adjustTop = adjustTop && window.getComputedStyle(end).
                    getPropertyValue('-moz-user-select') !== 'none';
//#endif
                if (adjustTop) {
                    var divBounds = div.getBoundingClientRect();
                    var r = Math.max(0, (e.pageY - divBounds.top) / divBounds.height);
                    end.style.top = (r * 100).toFixed(2) + '%';
                }
//#endif
                end.classList.add('active');
            });
            div.addEventListener('mouseup', function (e) {
                var end = div.querySelector('.endOfContent');
                if (!end) {
                    return;
                }
//#if !(MOZCENTRAL || FIREFOX)
                end.style.top = '';
//#endif
                end.classList.remove('active');
            });
        },
  * */

}
